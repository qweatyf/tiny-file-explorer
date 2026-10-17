(function () {
    'use strict';

    var scene, camera, renderer;
    var lineGreen, lineRed, lineCyan;
    var particles;
    var historyGreen = new Array(60).fill(0);
    var historyRed = new Array(60).fill(0);
    var historyCyan = new Array(60).fill(0);
    var maxPoints = 60;
    var container = document.getElementById('canvas-container');

    var msgTitle = document.getElementById('lbl-title');
    var msgMain = document.getElementById('val-main');
    var msgSub1 = document.getElementById('lbl-sub1');
    var msgSub2 = document.getElementById('lbl-sub2');
    var valSub1 = document.getElementById('val-sub1');
    var valSub2 = document.getElementById('val-sub2');
    var legendRow = document.getElementById('legend-row');
    var legGreen = document.getElementById('leg-green');
    var legRed = document.getElementById('leg-red');
    var legCyan = document.getElementById('leg-cyan');
    var appList = document.getElementById('app-list');

    var strings = {};

    function init() {
        try {
            scene = new THREE.Scene();
            scene.fog = new THREE.FogExp2(0x000814, 0.035);

            var w = container.clientWidth;
            var h = container.clientHeight;

            camera = new THREE.PerspectiveCamera(60, w / h, 0.1, 1000);
            camera.position.set(0, 4, 12);
            camera.lookAt(0, 0, 0);

            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
            renderer.setSize(w, h);
            renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
            container.appendChild(renderer.domElement);

            buildGrid();
            lineGreen = buildLine(0x00ff66);
            lineRed = buildLine(0xff3355);
            lineCyan = buildLine(0x00ffff);
            buildParticles();

            window.addEventListener('resize', onResize);
            animate();
        } catch (e) {
            showToast('3D init failed: ' + e.message);
            if (window.AndroidBridge && window.AndroidBridge.onWebGLError) {
                window.AndroidBridge.onWebGLError(e.message);
            }
        }
    }

    function buildGrid() {
        var grid = new THREE.GridHelper(20, 20, 0x00ffcc, 0x005566);
        grid.position.y = -2;
        grid.material.opacity = 0.35;
        grid.material.transparent = true;
        scene.add(grid);
    }

    function buildLine(color) {
        var geo = new THREE.BufferGeometry();
        var positions = new Float32Array(maxPoints * 3);
        for (var i = 0; i < maxPoints; i++) {
            positions[i * 3] = (i / (maxPoints - 1)) * 18 - 9;
            positions[i * 3 + 1] = -2;
            positions[i * 3 + 2] = 0;
        }
        geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));

        var mat = new THREE.LineBasicMaterial({ color: color });
        var line = new THREE.Line(geo, mat);
        scene.add(line);
        return line;
    }

    function buildParticles() {
        var count = 200;
        var geo = new THREE.BufferGeometry();
        var pos = new Float32Array(count * 3);
        for (var i = 0; i < count; i++) {
            pos[i * 3] = (Math.random() - 0.5) * 30;
            pos[i * 3 + 1] = Math.random() * 10 - 2;
            pos[i * 3 + 2] = (Math.random() - 0.5) * 20;
        }
        geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
        var mat = new THREE.PointsMaterial({
            color: 0x00ffcc,
            size: 0.08,
            transparent: true,
            opacity: 0.6
        });
        particles = new THREE.Points(geo, mat);
        scene.add(particles);
    }

    function onResize() {
        var w = container.clientWidth;
        var h = container.clientHeight;
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        renderer.setSize(w, h);
    }

    var t = 0;
    function animate() {
        requestAnimationFrame(animate);
        t += 0.01;
        if (particles) particles.rotation.y = t * 0.1;
        renderer.render(scene, camera);
    }

    function updateLine(line, hist, maxVal) {
        if (!line) return;
        var pos = line.geometry.attributes.position.array;
        for (var i = 0; i < maxPoints; i++) {
            var v = hist[i] / maxVal;
            if (v > 1) v = 1;
            if (v < 0) v = 0;
            pos[i * 3 + 1] = v * 6 - 2;
        }
        line.geometry.attributes.position.needsUpdate = true;
    }

    function shift(arr, val) {
        arr.shift();
        arr.push(val);
    }

    function showToast(text) {
        var el = document.getElementById('toast');
        el.textContent = text;
        el.classList.add('show');
        setTimeout(function () { el.classList.remove('show'); }, 1500);
    }

    function renderAppList(apps) {
        if (!appList) return;
        appList.innerHTML = '';
        if (!apps || apps.length === 0) {
            appList.innerHTML = '<div style="padding:20px;text-align:center;opacity:0.6;">'
                + (strings.no_data || '') + '</div>';
            return;
        }
        apps.forEach(function (app) {
            var item = document.createElement('div');
            item.className = 'app-item';

            var name = document.createElement('div');
            name.className = 'name';
            name.textContent = app.name;
            item.appendChild(name);

            var barWrap = document.createElement('div');
            barWrap.className = 'bar-wrap';
            var bar = document.createElement('div');
            bar.className = 'bar';
            bar.style.width = Math.min(100, app.value) + '%';
            barWrap.appendChild(bar);
            item.appendChild(barWrap);

            var pct = document.createElement('div');
            pct.className = 'pct';
            pct.textContent = app.value.toFixed(1) + '%';
            item.appendChild(pct);

            item.addEventListener('click', function () {
                if (window.AndroidBridge && window.AndroidBridge.onAppClicked) {
                    window.AndroidBridge.onAppClicked(app.pkg, app.name);
                }
            });

            appList.appendChild(item);
        });
    }

    window.setStrings = function (json) {
        try {
            strings = typeof json === 'string' ? JSON.parse(json) : json;
            if (legGreen) legGreen.textContent = strings.current || 'Current';
            if (legRed) legRed.textContent = strings.voltage || 'Voltage';
            if (legCyan) legCyan.textContent = strings.avg || 'Avg';
        } catch (e) {
            strings = {};
        }
    };

    window.updateCpu = function (json) {
        try {
            var d = typeof json === 'string' ? JSON.parse(json) : json;
            msgTitle.textContent = strings.cpu_title || '';
            msgMain.textContent = d.usage.toFixed(1) + '%';
            msgSub1.textContent = strings.cores || '';
            valSub1.textContent = d.cores;
            msgSub2.textContent = strings.temp || '';
            valSub2.textContent = d.temp + '\u00B0C';

            if (legendRow) legendRow.style.display = 'none';
            if (appList) appList.style.display = 'block';

            shift(historyCyan, d.usage);
            updateLine(lineCyan, historyCyan, 100);

            if (lineGreen) lineGreen.visible = false;
            if (lineRed) lineRed.visible = false;
            if (lineCyan) lineCyan.visible = true;

            renderAppList(d.apps || []);
        } catch (e) {
            showToast(e.message);
        }
    };

    window.updateBattery = function (json) {
        try {
            var d = typeof json === 'string' ? JSON.parse(json) : json;
            msgTitle.textContent = strings.battery_title || '';
            msgMain.textContent = d.level + '%';
            msgSub1.textContent = strings.current || '';
            valSub1.textContent = d.current + ' mA';
            msgSub2.textContent = strings.voltage || '';
            valSub2.textContent = d.voltage + ' mV';

            if (legendRow) legendRow.style.display = 'flex';
            if (appList) appList.style.display = 'none';

            shift(historyGreen, Math.abs(d.current));
            shift(historyRed, d.voltage);
            var avg = (Math.abs(d.current) + d.voltage) / 2;
            shift(historyCyan, avg);

            updateLine(lineGreen, historyGreen, 3000);
            updateLine(lineRed, historyRed, 5000);
            updateLine(lineCyan, historyCyan, 4000);

            if (lineGreen) lineGreen.visible = true;
            if (lineRed) lineRed.visible = true;
            if (lineCyan) lineCyan.visible = true;
        } catch (e) {
            showToast(e.message);
        }
    };

    window.showToast = showToast;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();