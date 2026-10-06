package com.example.myempty.activity2;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinyinUtils {

    private static final String COMMON =
        "的一是了我不人在他有这上们来到时大地为子中你说生国年着就那和要她出也得里后自以会家可下而过天去能对小多然于心学么之都好看起发当没成只如事把还用第样道想作种开美总从无情己面最女但现前些所同日手又行意动方期它头经长儿回位分爱老因很给名法间斯知世什两次使身者被高已亲其进此话常与活正感" +
        "见明问力理尔点文几定本公特做外孩相西果走将月十实向声车全信重三机工物气每并别真打太新比才便夫再书部水像眼等体却加电主界门利海受听表德少克代员许稜先口由死安写性马光白或住难望教命花结乐色更拉东神记处让母父应直字场平报友关放至张认接告入笑内英军候民岁往何度山觉路带万男边风解叫任金快原吃妈变通师立象数四失满战远格士音轻目条呢病始达深完今提求清王化空业思切怎非找片罗钱紶吗语元喜曾离飞科言干流欢约各即指合反题必该论交终林请医晚制球决窢传画保读运及则房早院量苦火布品近坐产答星精视五连司巴奇管类未朋且婚台夜青北队久乎越观落尽形影红爸百令周吧识步希亚术留市半热送兴造谈容极随演收首根讲整式取照办强石古华諣拿计您装似足双妻尼转诉米称丽客南领节衣站黑刻统断福城故历惊脸选包紧争另建维绝树系伤示愿持千史谁准联妇纪基买志静阿诗独复痛消社算义竟确酒需单治卡幸兰念举仅钟怕共毛句息功官待究跟穿室易游程号居考突皮哪费倒价图具刚脑永歌响商礼细专黄块脚味灵改据般破引食仍存众注笔甚某沉血备习校默务土微娘须试怀料调广蜖苏显赛查密议底列富梦错座参八除跑亮假印设线温虽掉京初养香停际致阳纸李纳验助激够严证帝饭忘趣支春集丈木研班普导顿睡展跳获艺六波察群皇段急庭创区奥器谢弟店否害草排背止组州朝封睛板角况曲馆育忙质河续哥呼若推境遇雨标姐充围案伦护冷警贝著雪索剧啊船险烟依斗值帮汉慢佛肯闻唱沙局伯族低玩资屋击速顾泪洲团圣旁堂兵七露园牛哭旅街劳型烈姑陈莫鱼异抱宝权鲁简态级票怪寻杀律胜份汽右洋范床舞秘午登楼贵吸责例追较职属渐左录丝牙党继托赶章智冲叶胡吉卖坚喝肉遗救修松临藏担戏善卫药悲敢靠伊村戴词森耳差短祖云规窗散迷油旧适乡架恩投弹铁博雷府压超负勒杂醒洗采毫嘴毕九冰既状乱景席珍童顶派素脱农疑练野按犯拍征坏骨余承置臂彩灯巨琴免环姆暗换技翻束增忍餐洛塞缺忆判欧层付阵玛批岛项狗休懂武革良恶恋委拥娜妙探呀营退摇弄桌熟诺宣银势奖宫忽套康供优课鸟喊降夏困刘罪亡鞋健模败伴守挥鲜财孤枪禁恐伙杰迹妹藸遍盖副坦牌江顺秋萨菜划授归浪听凡预奶雄升碃编典袋莱含盛济蒙棋端腿招释介烧误" +
        "住泽仓润例渐警坚舞康典惠永振键修洛维诗晓陵誉含昭骏宁羽峰宣塔律柏殿誉俊梅瑞乐邦逸硕冠威德翰恩辉恒航朗彦弘朗劲皓政轩迎诚铭羽航楠杰誉钦涛坤宸鸿杰誉瑾宁耀荣嘉瀚瑞轩仪豪峰然天宇航翔鹤鹏杰誉铭泽辉承轩浩铭羽航楠杰誉钦涛坤宸鸿杰誉瑾宁耀荣嘉瀚瑞轩仪豪峰然";

    private static final Map<String, List<Character>> COMMON_MAP = new HashMap<>();

    static {
        HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
        fmt.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        fmt.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (char c : COMMON.toCharArray()) {
            try {
                String[] py = PinyinHelper.toHanyuPinyinStringArray(c, fmt);
                if (py == null || py.length == 0) continue;
                String p = py[0];
                List<Character> list = COMMON_MAP.get(p);
                if (list == null) {
                    list = new ArrayList<>();
                    COMMON_MAP.put(p, list);
                }
                if (!list.contains(c)) list.add(c);
            } catch (Exception ignored) {}
        }
    }

    public static String toPinyin(String text) {
        StringBuilder sb = new StringBuilder();
        HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
        fmt.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        fmt.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);

        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) {
                try {
                    String[] py = PinyinHelper.toHanyuPinyinStringArray(c, fmt);
                    if (py != null && py.length > 0) {
                        sb.append(py[0]).append(" ");
                        continue;
                    }
                } catch (Exception ignored) {}
                sb.append(c).append(" ");
            } else {
                sb.append(c).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static String fromPinyin(String pinyin) {
        String target = pinyin.trim().toLowerCase();
        if (target.isEmpty()) return "没找到同音字";

        List<Character> common = COMMON_MAP.get(target);
        StringBuilder sb = new StringBuilder();
        int count = 0;

        if (common != null && !common.isEmpty()) {
            for (char c : common) {
                if (count > 0) sb.append(" ");
                sb.append(c);
                count++;
                if (count >= 30) break;
            }
        }

        HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
        fmt.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        fmt.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        for (char c = 0x4E00; c <= 0x9FA5 && count < 30; c++) {
            if (common != null && common.contains(c)) continue;
            try {
                String[] py = PinyinHelper.toHanyuPinyinStringArray(c, fmt);
                if (py != null) {
                    for (String p : py) {
                        if (p.equalsIgnoreCase(target)) {
                            if (count > 0) sb.append(" ");
                            sb.append(c);
                            count++;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (count == 0) return "没找到同音字";
        return sb.toString().trim();
    }
}