(function () {
    'use strict';

    var scene, camera, renderer;
    var earthGroup, earthMesh, marker;
    var countriesLines, latLines, lonLines, magLines, equatorLine, primeLine;
    var container = document.getElementById('earth-container');
    var labelSprites = [];
    var markerSprites = [];

    var latLabel = document.getElementById('earth-lat-label');
    var lonLabel = document.getElementById('earth-lon-label');
    var zoomLabel = document.getElementById('earth-zoom-label');
    var latVal = document.getElementById('earth-lat-val');
    var lonVal = document.getElementById('earth-lon-val');
    var zoomVal = document.getElementById('earth-zoom-val');
    var hintEl = document.getElementById('earth-hint');
    var btnLat = document.getElementById('btn-lat');
    var btnLon = document.getElementById('btn-lon');
    var btnMag = document.getElementById('btn-mag');
    var btnSave = document.getElementById('btn-save');
    var btnShot = document.getElementById('btn-shot');

    var strings = {};
    var labels = { countries: [], oceans: [] };
    var labelsLoaded = false;

    var radius = 5;
    var distance = 14;
    var dragging = false;
    var lastX = 0, lastY = 0;
    var pinchStart = 0;
    var pinchDist = 0;

    var showLat = true;
    var showLon = false;
    var showMag = false;

    var lastBuildDistance = -1;

    var longPressTimer = null;
    var longPressStartX = 0;
    var longPressStartY = 0;
    var longPressFired = false;

    function init() {
        try {
            scene = new THREE.Scene();

            var w = container.clientWidth;
            var h = container.clientHeight;

            camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 1000);
            camera.position.set(0, 0, distance);
            camera.lookAt(0, 0, 0);

            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
            renderer.setSize(w, h);
            renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
            container.appendChild(renderer.domElement);

            earthGroup = new THREE.Group();
            scene.add(earthGroup);

            buildEarth();
            buildStars();
            buildGridLines();
            loadWorld();

            bindTouch();
            bindButtons();
            window.addEventListener('resize', onResize);
            animate();

            setTimeout(tryLoadLabels, 300);
        } catch (e) {
            console.log("init error: " + e.message);
        }
    }

    function tryLoadLabels() {
        if (labelsLoaded) return;
        if (window.AndroidBridge && window.AndroidBridge.getLabels) {
            try {
                var json = window.AndroidBridge.getLabels();
                if (json && json.indexOf("countries") > 0) {
                    var d = JSON.parse(json);
                    if (d && d.countries && d.countries.length > 0) {
                        labels = d;
                        labelsLoaded = true;
                        buildLabelSprites();
                    }
                }
            } catch (e) {
                console.log("bridge labels error: " + e.message);
            }
        }
        if (!labelsLoaded) {
            setTimeout(tryLoadLabels, 500);
        }
    }

    function buildEarth() {
        var geo = new THREE.SphereGeometry(radius, 64, 64);
        var loader = new THREE.TextureLoader();
        loader.load('earth_texture.jpg', function (texture) {
            texture.minFilter = THREE.LinearFilter;
            texture.magFilter = THREE.LinearFilter;
            var mat = new THREE.MeshBasicMaterial({ map: texture });
            earthMesh = new THREE.Mesh(geo, mat);
            earthGroup.add(earthMesh);
        }, undefined, function () {
            var mat = new THREE.MeshBasicMaterial({ color: 0x0a2a4a });
            earthMesh = new THREE.Mesh(geo, mat);
            earthGroup.add(earthMesh);
        });
    }

    function buildStars() {
        var count = 400;
        var geo = new THREE.BufferGeometry();
        var pos = new Float32Array(count * 3);
        for (var i = 0; i < count; i++) {
            var r = 30 + Math.random() * 40;
            var theta = Math.random() * Math.PI * 2;
            var phi = Math.acos(2 * Math.random() - 1);
            pos[i * 3] = r * Math.sin(phi) * Math.cos(theta);
            pos[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
            pos[i * 3 + 2] = r * Math.cos(phi);
        }
        geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
        var mat = new THREE.PointsMaterial({
            color: 0x88ddff,
            size: 0.15,
            transparent: true,
            opacity: 0.7
        });
        scene.add(new THREE.Points(geo, mat));
    }

    function buildGridLines() {
        latLines = buildLatitudeLines();
        lonLines = buildLongitudeLines();
        magLines = buildMagneticLines();
        equatorLine = buildEquatorLine();
        primeLine = buildPrimeMeridianLine();

        earthGroup.add(latLines);
        earthGroup.add(lonLines);
        earthGroup.add(magLines);
        earthGroup.add(equatorLine);
        earthGroup.add(primeLine);

        latLines.visible = showLat;
        lonLines.visible = showLon;
        magLines.visible = showMag;
        equatorLine.visible = showLat;
        primeLine.visible = showLon;
    }

    function buildLatitudeLines() {
        var group = new THREE.Group();
        var r = radius * 1.04;
        var mat = new THREE.LineBasicMaterial({
            color: 0xffee00,
            transparent: true,
            opacity: 0.7
        });
        var lats = [30, -30, 60, -60];
        lats.forEach(function (lat) {
            var pts = [];
            for (var lon = -180; lon <= 180; lon += 3) {
                var p = latLonToVec3(lat, lon, r);
                pts.push(new THREE.Vector3(p.x, p.y, p.z));
            }
            var geo = new THREE.BufferGeometry().setFromPoints(pts);
            group.add(new THREE.Line(geo, mat));
        });
        return group;
    }

    function buildEquatorLine() {
        var r = radius * 1.045;
        var mat = new THREE.LineBasicMaterial({
            color: 0xff0000,
            transparent: true,
            opacity: 0.95
        });
        var pts = [];
        for (var lon = -180; lon <= 180; lon += 2) {
            var p = latLonToVec3(0, lon, r);
            pts.push(new THREE.Vector3(p.x, p.y, p.z));
        }
        var geo = new THREE.BufferGeometry().setFromPoints(pts);
        return new THREE.Line(geo, mat);
    }

    function buildLongitudeLines() {
        var group = new THREE.Group();
        var r = radius * 1.04;
        var mat = new THREE.LineBasicMaterial({
            color: 0xff8800,
            transparent: true,
            opacity: 0.7
        });
        var lons = [30, 60, 90, 120, 150, -30, -60, -90, -120, -150];
        lons.forEach(function (lon) {
            var pts = [];
            for (var lat = -90; lat <= 90; lat += 3) {
                var p = latLonToVec3(lat, lon, r);
                pts.push(new THREE.Vector3(p.x, p.y, p.z));
            }
            var geo = new THREE.BufferGeometry().setFromPoints(pts);
            group.add(new THREE.Line(geo, mat));
        });
        return group;
    }

    function buildPrimeMeridianLine() {
        var r = radius * 1.045;
        var mat = new THREE.LineBasicMaterial({
            color: 0xff0000,
            transparent: true,
            opacity: 0.95
        });
        var pts = [];
        for (var lat = -90; lat <= 90; lat += 2) {
            var p = latLonToVec3(lat, 0, r);
            pts.push(new THREE.Vector3(p.x, p.y, p.z));
        }
        var geo = new THREE.BufferGeometry().setFromPoints(pts);
        return new THREE.Line(geo, mat);
    }

    function buildMagneticLines() {
        var group = new THREE.Group();
        var mat = new THREE.LineBasicMaterial({
            color: 0xff33cc,
            transparent: true,
            opacity: 0.6
        });
        var magPoleLat = 80;
        var lines = 12;
        for (var i = 0; i < lines; i++) {
            var lon = (360.0 / lines) * i - 180;
            var pts = [];
            for (var t = 0; t <= 1.001; t += 0.02) {
                var angle = Math.PI * t;
                var r = radius * 1.5 + 1.5 * Math.sin(angle);
                var lat = magPoleLat - (2 * magPoleLat) * t;
                var lonAt = lon + 40 * Math.sin(angle);
                var p = latLonToVec3(lat, lonAt, r);
                pts.push(new THREE.Vector3(p.x, p.y, p.z));
            }
            var geo = new THREE.BufferGeometry().setFromPoints(pts);
            group.add(new THREE.Line(geo, mat));
        }
        return group;
    }

    function nameToColor(name) {
        var hash = 0;
        for (var i = 0; i < name.length; i++) {
            hash = (hash * 31 + name.charCodeAt(i)) & 0xFFFFFF;
        }
        var r = ((hash >> 16) & 0xFF);
        var g = ((hash >> 8) & 0xFF);
        var b = (hash & 0xFF);
        r = Math.floor(r * 0.4 + 120);
        g = Math.floor(g * 0.4 + 120);
        b = Math.floor(b * 0.4 + 120);
        return (r << 16) | (g << 8) | b;
    }

    function loadWorld() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'world.json', true);
        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                if (xhr.status === 200 || xhr.status === 0) {
                    try {
                        var data = JSON.parse(xhr.responseText);
                        countriesLines = drawCountries(data);
                        earthGroup.add(countriesLines);
                    } catch (e) {
                        console.log("world.json error: " + e.message);
                    }
                }
            }
        };
        xhr.send();
    }

    function drawCountries(data) {
        var group = new THREE.Group();
        var features = data.features || [];
        features.forEach(function (feat) {
            var geom = feat.geometry;
            if (!geom) return;

            var props = feat.properties || {};
            var nameZh = props.NAME_ZH || props.NAME || "";
            var iso = props.ISO_A3 || "";
            var isChina = (nameZh === "中国" || iso === "CHN");

            var color;
            if (isChina) {
                color = 0xff0000;
            } else {
                color = nameToColor(nameZh || iso);
            }

            var mat = new THREE.LineBasicMaterial({
                color: color,
                transparent: true,
                opacity: 0.95
            });

            if (geom.type === 'Polygon') {
                geom.coordinates.forEach(function (ring) {
                    group.add(ringLine(ring, mat));
                });
            } else if (geom.type === 'MultiPolygon') {
                geom.coordinates.forEach(function (poly) {
                    poly.forEach(function (ring) {
                        group.add(ringLine(ring, mat));
                    });
                });
            }
        });
        return group;
    }

    function ringLine(ring, mat) {
        var pts = [];
        var r = radius * 1.02;
        for (var i = 0; i < ring.length; i++) {
            var p = latLonToVec3(ring[i][1], ring[i][0], r);
            pts.push(new THREE.Vector3(p.x, p.y, p.z));
        }
        var geo = new THREE.BufferGeometry().setFromPoints(pts);
        return new THREE.Line(geo, mat);
    }

    function makeLabelTexture(text) {
        var canvas = document.createElement('canvas');
        var ctx = canvas.getContext('2d');

        var fontSize = 44;
        var pad = 10;
        ctx.font = fontSize + 'px sans-serif';
        var metrics = ctx.measureText(text);
        var textW = Math.ceil(metrics.width);

        canvas.width = textW + pad * 2;
        canvas.height = fontSize + pad * 2;

        ctx.font = fontSize + 'px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        ctx.fillStyle = 'rgba(0, 0, 0, 0.55)';
        var rx = pad / 2;
        var ry = pad / 2;
        var rw = canvas.width - pad;
        var rh = canvas.height - pad;
        var cr = 10;
        ctx.beginPath();
        ctx.moveTo(rx + cr, ry);
        ctx.lineTo(rx + rw - cr, ry);
        ctx.quadraticCurveTo(rx + rw, ry, rx + rw, ry + cr);
        ctx.lineTo(rx + rw, ry + rh - cr);
        ctx.quadraticCurveTo(rx + rw, ry + rh, rx + rw - cr, ry + rh);
        ctx.lineTo(rx + cr, ry + rh);
        ctx.quadraticCurveTo(rx, ry + rh, rx, ry + rh - cr);
        ctx.lineTo(rx, ry + cr);
        ctx.quadraticCurveTo(rx, ry, rx + cr, ry);
        ctx.closePath();
        ctx.fill();

        ctx.fillStyle = '#ffffff';
        ctx.fillText(text, canvas.width / 2, canvas.height / 2);

        var texture = new THREE.CanvasTexture(canvas);
        texture.minFilter = THREE.LinearFilter;
        texture.magFilter = THREE.LinearFilter;
        return texture;
    }

    function addLabelSprite(name, lat, lon) {
        var texture = makeLabelTexture(name);
        var mat = new THREE.SpriteMaterial({
            map: texture,
            transparent: true,
            depthTest: true,
            depthWrite: false
        });
        var sprite = new THREE.Sprite(mat);

        var aspect = texture.image.width / texture.image.height;
        var h = 0.32;
        var w = h * aspect;
        sprite.scale.set(w, h, 1);

        var p = latLonToVec3(lat, lon, radius * 1.005);
        sprite.position.set(p.x, p.y, p.z);

        earthGroup.add(sprite);
        labelSprites.push(sprite);
    }

    function projectedScreenPos(lat, lon) {
        var p = latLonToVec3(lat, lon, radius * 1.005);
        var v = new THREE.Vector3(p.x, p.y, p.z);
        v.applyMatrix4(earthGroup.matrixWorld);

        var worldLen = v.length();
        var camLen = camera.position.length();
        if (worldLen > 0 && camLen > 0) {
            var dot = v.dot(camera.position) / (worldLen * camLen);
            if (dot < 0.15) return null;
        }

        var projected = v.clone().project(camera);
        return {
            x: (projected.x * 0.5 + 0.5) * container.clientWidth,
            y: (-projected.y * 0.5 + 0.5) * container.clientHeight
        };
    }

    function currentMinPx() {
        var minPx = 24;
        var maxPx = 100;
        var dMin = 8;
        var dMax = 30;
        if (distance <= dMin) return minPx;
        if (distance >= dMax) return maxPx;
        var t = (distance - dMin) / (dMax - dMin);
        return minPx + (maxPx - minPx) * t;
    }

    function buildLabelSprites() {
        for (var i = 0; i < labelSprites.length; i++) {
            earthGroup.remove(labelSprites[i]);
        }
        labelSprites = [];

        if (!labels.countries || labels.countries.length === 0) {
            console.log("buildLabelSprites: no countries");
            return;
        }

        var minPx = currentMinPx();
        var minPx2 = minPx * minPx;
        var keptPos = [];
        var keptCount = 0;

        for (var i = 0; i < labels.countries.length; i++) {
            var c = labels.countries[i];
            var pos = projectedScreenPos(c.lat, c.lon);
            if (pos === null) continue;

            var tooClose = false;
            for (var j = 0; j < keptPos.length; j++) {
                var dx = pos.x - keptPos[j].x;
                var dy = pos.y - keptPos[j].y;
                if (dx * dx + dy * dy < minPx2) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            keptPos.push(pos);
            addLabelSprite(c.name, c.lat, c.lon);
            keptCount++;
        }

        lastBuildDistance = distance;
        console.log("buildLabelSprites: kept=" + keptCount + "/" + labels.countries.length + " minPx=" + minPx.toFixed(1));
    }

    window.loadLabels = function (json) {
        try {
            var d = typeof json === 'string' ? JSON.parse(json) : json;
            if (d && d.countries && d.countries.length > 0) {
                labels = d;
                labelsLoaded = true;
                buildLabelSprites();
            }
        } catch (e) {
            console.log("loadLabels error: " + e.message);
        }
    };

    function makeMarkerSprite(text) {
        var canvas = document.createElement('canvas');
        var ctx = canvas.getContext('2d');
        canvas.width = 64; canvas.height = 64;
        ctx.fillStyle = '#ff3366';
        ctx.beginPath();
        ctx.arc(32, 32, 20, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 4;
        ctx.stroke();
        return new THREE.CanvasTexture(canvas);
    }

    function addMarkerSprite(name, lat, lon, save) {
        var texture = makeMarkerSprite(name);
        var mat = new THREE.SpriteMaterial({
            map: texture,
            transparent: true,
            depthTest: false
        });
        var sprite = new THREE.Sprite(mat);
        sprite.scale.set(0.5, 0.5, 1);
        var p = latLonToVec3(lat, lon, radius * 1.02);
        sprite.position.set(p.x, p.y, p.z);
        sprite.userData = { name: name, lat: lat, lon: lon };
        earthGroup.add(sprite);
        markerSprites.push(sprite);
        
        if (save) saveMarkers();
    }

    function saveMarkers() {
        if (!window.AndroidBridge || !window.AndroidBridge.saveMarkers) return;
        var arr = [];
        for (var i = 0; i < markerSprites.length; i++) {
            var u = markerSprites[i].userData;
            arr.push({ name: u.name, lat: u.lat, lon: u.lon });
        }
        try { window.AndroidBridge.saveMarkers(JSON.stringify(arr)); } catch(e){}
    }

    window.loadMarkers = function (json) {
        try {
            var arr = typeof json === 'string' ? JSON.parse(json) : json;
            for (var i = 0; i < arr.length; i++) {
                addMarkerSprite(arr[i].name, arr[i].lat, arr[i].lon, false);
            }
        } catch (e) {}
    };

    function bindButtons() {
        btnLat.addEventListener('click', function () {
            showLat = !showLat;
            if (latLines) latLines.visible = showLat;
            if (equatorLine) equatorLine.visible = showLat;
            btnLat.classList.toggle('active', showLat);
        });
        btnLon.addEventListener('click', function () {
            showLon = !showLon;
            if (lonLines) lonLines.visible = showLon;
            if (primeLine) primeLine.visible = showLon;
            btnLon.classList.toggle('active', showLon);
        });
        btnMag.addEventListener('click', function () {
            showMag = !showMag;
            if (magLines) magLines.visible = showMag;
            btnMag.classList.toggle('active', showMag);
        });

        btnLat.classList.toggle('active', showLat);
        btnLon.classList.toggle('active', showLon);
        btnMag.classList.toggle('active', showMag);

        btnSave.addEventListener('click', function () {
            var lat = parseFloat(latVal.textContent);
            var lon = parseFloat(lonVal.textContent);
            if (isNaN(lat) || isNaN(lon)) return;
            addMarkerSprite("Mark", lat, lon, true);
        });

        btnShot.addEventListener('click', function () {
            if (window.AndroidBridge && window.AndroidBridge.exportScreenshot) {
                var data = renderer.domElement.toDataURL("image/png");
                window.AndroidBridge.exportScreenshot(data);
            }
        });
    }

    function latLonToVec3(lat, lon, r) {
        var phi = (90 - lat) * Math.PI / 180;
        var theta = (lon + 180) * Math.PI / 180;
        var x = -r * Math.sin(phi) * Math.cos(theta);
        var z = r * Math.sin(phi) * Math.sin(theta);
        var y = r * Math.cos(phi);
        return { x: x, y: y, z: z };
    }

    function vec3ToLatLon(v) {
        var len = v.length();
        if (len === 0) return { lat: 0, lon: 0 };
        var nx = v.x / len;
        var ny = v.y / len;
        var nz = v.z / len;
        var lat = 90 - Math.acos(ny) * 180 / Math.PI;
        var theta = Math.atan2(nz, -nx);
        var lon = theta * 180 / Math.PI - 180;
        while (lon < -180) lon += 360;
        while (lon > 180) lon -= 360;
        return { lat: lat, lon: lon };
    }

    function setMarker(lat, lon) {
        if (marker) {
            earthGroup.remove(marker);
            marker = null;
        }
        var p = latLonToVec3(lat, lon, radius * 1.02);
        var geo = new THREE.SphereGeometry(0.12, 12, 12);
        var mat = new THREE.MeshBasicMaterial({ color: 0xff3366 });
        marker = new THREE.Mesh(geo, mat);
        marker.position.set(p.x, p.y, p.z);
        earthGroup.add(marker);
        latVal.textContent = lat.toFixed(4);
        lonVal.textContent = lon.toFixed(4);
    }

    function screenToSphereLatLon(clientX, clientY) {
        var rect = renderer.domElement.getBoundingClientRect();
        var x = ((clientX - rect.left) / rect.width) * 2 - 1;
        var y = -((clientY - rect.top) / rect.height) * 2 + 1;

        var raycaster = new THREE.Raycaster();
        raycaster.setFromCamera(new THREE.Vector2(x, y), camera);

        var sphere = new THREE.Sphere(new THREE.Vector3(0, 0, 0), radius);
        var point = new THREE.Vector3();
        var hit = raycaster.ray.intersectSphere(sphere, point);
        if (!hit) return null;

        var local = point.clone();
        earthGroup.worldToLocal(local);
        return vec3ToLatLon(local);
    }

    function pickLatLon(clientX, clientY) {
        var res = screenToSphereLatLon(clientX, clientY);
        if (!res) return;
        setMarker(res.lat, res.lon);
        if (window.AndroidBridge && window.AndroidBridge.onPick) {
            try {
                var payload = JSON.stringify({ lat: res.lat, lon: res.lon });
                window.AndroidBridge.onPick(payload);
            } catch (e) {}
        }
    }

    function clearLongPress() {
        if (longPressTimer) {
            clearTimeout(longPressTimer);
            longPressTimer = null;
        }
    }

    function startLongPress(clientX, clientY) {
        clearLongPress();
        longPressStartX = clientX;
        longPressStartY = clientY;
        longPressFired = false;
        longPressTimer = setTimeout(function () {
            longPressFired = true;
            pickLatLon(longPressStartX, longPressStartY);
        }, 500);
    }

    function cancelLongPressOnMove(clientX, clientY) {
        if (!longPressTimer) return;
        var dx = clientX - longPressStartX;
        var dy = clientY - longPressStartY;
        if (dx * dx + dy * dy > 12 * 12) {
            clearLongPress();
        }
    }

    function bindTouch() {
        var el = renderer.domElement;

        el.addEventListener('touchstart', function (e) {
            if (e.touches.length === 1) {
                dragging = true;
                lastX = e.touches[0].clientX;
                lastY = e.touches[0].clientY;
                startLongPress(e.touches[0].clientX, e.touches[0].clientY);
            } else if (e.touches.length === 2) {
                dragging = false;
                clearLongPress();
                pinchDist = pinchLength(e.touches);
                pinchStart = distance;
            }
        }, { passive: true });

        el.addEventListener('touchmove', function (e) {
            if (e.touches.length === 1 && dragging) {
                var dx = e.touches[0].clientX - lastX;
                var dy = e.touches[0].clientY - lastY;
                earthGroup.rotation.y += dx * 0.01;
                earthGroup.rotation.x += dy * 0.01;
                if (earthGroup.rotation.x > Math.PI / 2) earthGroup.rotation.x = Math.PI / 2;
                if (earthGroup.rotation.x < -Math.PI / 2) earthGroup.rotation.x = -Math.PI / 2;
                lastX = e.touches[0].clientX;
                lastY = e.touches[0].clientY;
                cancelLongPressOnMove(e.touches[0].clientX, e.touches[0].clientY);
            } else if (e.touches.length === 2) {
                var d = pinchLength(e.touches);
                if (pinchDist > 0) {
                    var scale = pinchDist / d;
                    distance = pinchStart * scale;
                    if (distance < 8) distance = 8;
                    if (distance > 30) distance = 30;
                    camera.position.z = distance;
                }
                zoomVal.textContent = distance.toFixed(1);
            }
            e.preventDefault();
        }, { passive: false });

        el.addEventListener('touchend', function () {
            dragging = false;
            clearLongPress();
            if (labelsLoaded && Math.abs(distance - lastBuildDistance) > 1.5) {
                buildLabelSprites();
            }
        }, { passive: true });

        el.addEventListener('touchcancel', function () {
            dragging = false;
            clearLongPress();
        }, { passive: true });

        el.addEventListener('mousedown', function (e) {
            dragging = true;
            lastX = e.clientX;
            lastY = e.clientY;
            startLongPress(e.clientX, e.clientY);
        });

        el.addEventListener('mousemove', function (e) {
            if (!dragging) return;
            var dx = e.clientX - lastX;
            var dy = e.clientY - lastY;
            earthGroup.rotation.y += dx * 0.01;
            earthGroup.rotation.x += dy * 0.01;
            if (earthGroup.rotation.x > Math.PI / 2) earthGroup.rotation.x = Math.PI / 2;
            if (earthGroup.rotation.x < -Math.PI / 2) earthGroup.rotation.x = -Math.PI / 2;
            lastX = e.clientX;
            lastY = e.clientY;
            cancelLongPressOnMove(e.clientX, e.clientY);
        });

        el.addEventListener('mouseup', function () {
            dragging = false;
            clearLongPress();
            if (labelsLoaded && Math.abs(distance - lastBuildDistance) > 1.5) {
                buildLabelSprites();
            }
        });

        el.addEventListener('mouseleave', function () {
            dragging = false;
            clearLongPress();
        });

        el.addEventListener('contextmenu', function (e) {
            e.preventDefault();
        });
    }

    function pinchLength(touches) {
        var dx = touches[0].clientX - touches[1].clientX;
        var dy = touches[0].clientY - touches[1].clientY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    function onResize() {
        var w = container.clientWidth;
        var h = container.clientHeight;
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        renderer.setSize(w, h);
    }

    function animate() {
        requestAnimationFrame(animate);
        renderer.render(scene, camera);
    }

    window.setStrings = function (json) {
        try {
            strings = typeof json === 'string' ? JSON.parse(json) : json;
            latLabel.textContent = strings.lat || 'Lat';
            lonLabel.textContent = strings.lon || 'Lon';
            zoomLabel.textContent = strings.zoom || 'Zoom';
            hintEl.textContent = strings.hint || '';
            btnLat.textContent = strings.btn_lat || 'Lat';
            btnLon.textContent = strings.btn_lon || 'Lon';
            btnMag.textContent = strings.btn_mag || 'Mag';
            btnSave.textContent = strings.btn_save || 'Save';
            btnShot.textContent = strings.btn_shot || 'Shot';
        } catch (e) {
            strings = {};
        }
    };

    window.locate = function (json) {
        try {
            var d = typeof json === 'string' ? JSON.parse(json) : json;
            setMarker(d.lat, d.lon);
        } catch (e) {}
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();