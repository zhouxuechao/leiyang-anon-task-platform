<script setup lang="ts">
import {
  Bell,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Edit3,
  Flame,
  Heart,
  Home,
  ImagePlus,
  Loader2,
  LogOut,
  ListMusic,
  Maximize2,
  MessageCircle,
  Minus,
  Music2,
  Pause,
  Play,
  Plus,
  QrCode,
  RefreshCw,
  Search,
  Send,
  Settings,
  Share2,
  SkipBack,
  SkipForward,
  Sparkles,
  Target,
  UserRound,
  WalletCards,
  X,
  Zap,
} from "lucide-vue-next";
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import QRCode from "qrcode";
import { api, clearToken, fileUrl, getToken, setToken, upload } from "./api";
import type {
  CommentItem,
  FollowSummary,
  FollowUser,
  HomeData,
  MessageItem,
  MusicCredits,
  MusicJob,
  MusicPackage,
  OrderItem,
  PlazaPost,
  PlazaMeta,
  RechargeConfig,
  SlicePage,
  TaskSubmission,
  TaskDetail,
  TaskItem,
  User,
  UserCard,
  Wallet,
  WalletFlow,
  WithdrawItem,
} from "./types";

type Tab = "plaza" | "tasks" | "music" | "me";
type MePanel = "overview" | "posts" | "orders" | "messages" | "wallet" | "follows";
type LoginMode = "password" | "code";
type PlayerSong = {
  id: number;
  title: string;
  audioUrl?: string;
  imageUrl?: string;
  authorName?: string;
  lyrics?: string;
  prompt?: string;
  style?: string;
  duration?: string;
  createdAt?: string;
  lyricist?: string;
  composer?: string;
};

const pageSize = 20;

const activeTab = ref<Tab>("plaza");
const mePanel = ref<MePanel>("overview");
const token = ref(getToken());
const me = ref<User | null>(null);
const toast = ref("");
const loading = ref(false);
const paging = ref(false);
const refreshing = ref(false);
const nowTs = ref(Date.now());
const homeData = ref<HomeData>({ banners: [], notices: [] });
let touchStartY = 0;
let touchStartAtTop = false;
let clockTimer: number | undefined;

const posts = ref<PlazaPost[]>([]);
const postPage = ref(1);
const postHasMore = ref(true);
const postSort = ref("HOT");
const postFilter = ref("ALL");
const plazaMeta = ref<PlazaMeta>({
  sorts: [],
  categories: [],
  aiProviders: [],
});
const composerOpen = ref(false);
const postContent = ref("");
const postImages = ref<string[]>([]);
const commentsOpen = ref(false);
const activePost = ref<PlazaPost | null>(null);
const comments = ref<CommentItem[]>([]);
const commentText = ref("");
const postDetailOpen = ref(false);
const detailPost = ref<PlazaPost | null>(null);
const detailComments = ref<CommentItem[]>([]);

const tasks = ref<TaskItem[]>([]);
const taskPage = ref(1);
const taskHasMore = ref(true);
const taskQuery = ref("");
const taskSort = ref("LATEST");

const musicJobs = ref<MusicJob[]>([]);
const musicLoaded = ref(false);
const musicSubmitting = ref(false);
const musicHistoryExpanded = ref(false);
const musicCredits = ref<MusicCredits | null>(null);
const musicPackages = ref<MusicPackage[]>([]);
const musicHall = ref<MusicJob[]>([]);
const musicHallSort = ref<"hot" | "new">("hot");
const playerAudio = ref<HTMLAudioElement | null>(null);
const currentSong = ref<PlayerSong | null>(null);
const playerOpen = ref(false);
const playerExpanded = ref(false);
const playerListOpen = ref(false);
const playerPlaying = ref(false);
const playerCurrentTime = ref(0);
const playerDuration = ref(0);
const playerLyricsRef = ref<HTMLElement | null>(null);
const musicGenerationVisual = reactive<Record<number, number>>({});
const musicGenerationTarget = reactive<Record<number, number>>({});
let musicPollTimer: number | undefined;
let musicProgressTimer: number | undefined;
let messagePollTimer: number | undefined;
let musicPollBusy = false;
const showAllMusicPacks = ref(false);
const musicStyleGroup = ref("genre");
const musicForm = ref({
  title: "",
  prompt: "",
  style: "pop, upbeat, electronic",
  custom_mode: false,
  instrumental: false,
  lang: "zh",
});
const lastMusicLyricTheme = ref("");
const lastMusicLyricTitle = ref("");
const lastMusicLyricOutput = ref("");
const taskDetailOpen = ref(false);
const taskDetail = ref<TaskDetail | null>(null);
const myOrder = ref<{ taskNo: string; orderNo: string; orderStatus: string; acceptTime?: string; submitTime?: string } | null>(null);
const taskSubmissions = ref<TaskSubmission[]>([]);
const submitProofOpen = ref(false);
const proofText = ref("");
const proofImages = ref<string[]>([]);
const publishOpen = ref(false);
const taskForm = ref({
  title: "",
  content: "",
  category: "同城互助",
  locationText: "线上",
  amount: "9.90",
  totalSlots: 10,
  deadlineAt: "",
  proofRequirements: "文字说明即可，图片可选",
});

const loginOpen = ref(false);
const loginMode = ref<LoginMode>("password");
const registerStep = ref<"none" | "password">("none");
const email = ref("");
const code = ref("");
const password = ref("");
const passwordConfirm = ref("");
const emailCodeExpireAt = ref(0);
const emailCodeLeft = computed(() => Math.max(0, Math.ceil((emailCodeExpireAt.value - nowTs.value) / 1000)));

const wallet = ref<Wallet | null>(null);
const walletFlows = ref<WalletFlow[]>([]);
const rechargeOpen = ref(false);
const rechargeAmount = ref("9.90");
const rechargeConfig = ref<RechargeConfig | null>(null);
const rechargeAfterTask = ref(false);
const rechargeAfterMusic = ref(false);
const rechargeAfterPackage = ref(false);
const rechargeQrDataUrl = ref("");
const rechargeOutTradeNo = ref("");
const rechargeQrStatus = ref<"pending" | "success" | "">("");
let rechargePollTimer: ReturnType<typeof setInterval> | null = null;
const packagePurchaseOpen = ref(false);
const selectedMusicPackage = ref<MusicPackage | null>(null);
const editProfileOpen = ref(false);
const profileForm = ref({ nickname: "", avatar: "", gender: "UNKNOWN", signature: "" });
const feedbackOpen = ref(false);
const feedbackForm = ref({ content: "", contact: "" });
const withdrawOpen = ref(false);
const withdrawForm = ref({ amount: "", channel: "WECHAT", qrCodeUrl: "" });
const myPosts = ref<PlazaPost[]>([]);
const myPostPage = ref(1);
const myPostHasMore = ref(true);
const myOrders = ref<OrderItem[]>([]);
const orderPage = ref(1);
const orderHasMore = ref(true);
const messages = ref<MessageItem[]>([]);
const unreadMessageCount = ref(0);
const withdraws = ref<WithdrawItem[]>([]);
const withdrawDetailOpen = ref(false);
const withdrawDetail = ref<WithdrawItem | null>(null);
const followSummary = ref<FollowSummary | null>(null);
const following = ref<FollowUser[]>([]);
const followers = ref<FollowUser[]>([]);
const userCardOpen = ref(false);
const userCard = ref<UserCard | null>(null);
const userCardPosts = ref<PlazaPost[]>([]);
const userCardTasks = ref<TaskItem[]>([]);
const userCardMusic = ref<MusicJob[]>([]);
const userCardView = ref<"home" | "post">("home");
const userCardContentTab = ref<"posts" | "tasks" | "music">("posts");
const userCardDetailPost = ref<PlazaPost | null>(null);
const userCardDetailComments = ref<CommentItem[]>([]);
const userCardCommentText = ref("");
const userPostCommentsOpen = reactive<Record<number, boolean>>({});
const userPostComments = reactive<Record<number, CommentItem[]>>({});
const userPostCommentText = reactive<Record<number, string>>({});
const userMusicCommentsOpen = reactive<Record<number, boolean>>({});
const userMusicComments = reactive<Record<number, CommentItem[]>>({});
const userMusicCommentText = reactive<Record<number, string>>({});

const isLoggedIn = computed(() => Boolean(token.value && me.value));
const frozenFlows = computed(() => walletFlows.value.filter((f) => f.type.includes("FREEZE") || f.type.includes("UNFREEZE") || f.type.includes("SETTLE")));
const ledgerFlows = computed(() => walletFlows.value.filter((f) => !f.type.includes("FREEZE") && !f.type.includes("UNFREEZE")));
const filterOptions = computed(() => {
  const base = [
    { code: "ALL", name: "全部" },
    { code: "MALE", name: "男生" },
    { code: "FEMALE", name: "女生" },
  ];
  const seen = new Set(base.map((v) => v.code));
  const rest = plazaMeta.value.categories
    .filter((c) => c.code && !seen.has(c.code) && c.code !== "AUTO")
    .map((c) => {
      seen.add(c.code);
      return c;
    });
  return [...base, ...rest];
});
const musicLanguages = [
  { value: "zh", label: "中文" },
  { value: "en", label: "English" },
  { value: "ja", label: "日本語" },
  { value: "ko", label: "한국어" },
  { value: "es", label: "Español" },
  { value: "fr", label: "Français" },
  { value: "de", label: "Deutsch" },
  { value: "it", label: "Italiano" },
  { value: "pt", label: "Português" },
  { value: "ru", label: "Русский" },
  { value: "id", label: "Indonesia" },
  { value: "vi", label: "Tiếng Việt" },
  { value: "th", label: "ไทย" },
  { value: "ar", label: "العربية" },
  { value: "hi", label: "हिन्दी" },
  { value: "tr", label: "Türkçe" },
];
const musicInspirationPacks = [
  { title: "失恋伤感", desc: "女声 · 慢节奏", prompt: "失恋后深夜一个人听的伤感情歌，女声，慢节奏" },
  { title: "给女朋友", desc: "温柔甜蜜 · 告白", prompt: "给女朋友的一首告白歌，温柔甜蜜，男声主唱，说出她最动心的话" },
  { title: "给男朋友", desc: "陪伴感谢 · 情歌", prompt: "给男朋友的一首情歌，女声温柔，感谢他一直陪伴" },
  { title: "回不去的曾经", desc: "催泪钢琴 · 回忆", prompt: "回忆曾经在一起的美好，现在却回不去了，催泪钢琴曲" },
  { title: "情人节告白", desc: "浪漫 · 心动", prompt: "情人节对TA说的情话，温暖浪漫，玫瑰与巧克力的心动" },
  { title: "深夜孤独", desc: "城市霓虹 · 迷茫", prompt: "一个人深夜走在空荡的街头，霓虹灯下的孤独和迷茫" },
  { title: "生日祝福", desc: "欢快温馨 · 祝福", prompt: "给最好的朋友唱一首生日快乐歌，欢快温馨，满满祝福" },
  { title: "深夜想家", desc: "异乡打拼 · 女声", prompt: "想家的深夜，在外地打拼的孤独感，温柔女声" },
  { title: "给妈妈", desc: "钢琴伴奏 · 温暖", prompt: "给妈妈的感恩歌曲，钢琴伴奏，温暖催泪" },
  { title: "给爸爸", desc: "深沉男声 · 父爱", prompt: "给爸爸的一首歌，父亲如山的爱，温暖深沉男声" },
  { title: "甜蜜爱情", desc: "治愈女声 · 温柔", prompt: "一首甜甜的爱情歌，女声，温柔治愈" },
  { title: "春节团圆", desc: "年夜饭 · 热闹", prompt: "一家人围坐吃年夜饭的温暖，新年红包和鞭炮声" },
  { title: "满屋遗憾", desc: "离开 · 回忆", prompt: "爱过的人已经离开，留下满屋子的回忆和遗憾" },
  { title: "初恋心动", desc: "青涩 · 甜蜜", prompt: "第一次牵手的紧张和甜蜜，青涩又美好" },
  { title: "婚礼誓言", desc: "誓言 · 感人", prompt: "婚礼上的誓言，我愿意陪你到老，感人至深" },
  { title: "给宝宝", desc: "摇篮曲 · 安静", prompt: "给宝宝的温柔摇篮曲，轻柔钢琴，温馨安静" },
  { title: "520表白", desc: "心跳加速 · 喜欢", prompt: "520这天说出藏了很久的喜欢，心跳加速" },
  { title: "七夕浪漫", desc: "古风 · 中式告白", prompt: "七夕之夜的中式告白，鹊桥相会，古风浪漫" },
  { title: "给爷爷奶奶", desc: "岁月 · 陪伴", prompt: "院子里的摇椅和蒲扇，爷爷奶奶的慈祥岁月" },
  { title: "中秋月圆", desc: "思念 · 团圆", prompt: "中秋月下，月饼和远方的家人，思念与团圆" },
  { title: "求婚时刻", desc: "承诺 · 选择你", prompt: "求婚时刻的心跳与承诺，今生选择你" },
  { title: "给老师", desc: "毕业 · 感恩", prompt: "毕业时给老师的感恩之歌，黑板上的粉笔字" },
  { title: "暧昧心跳", desc: "若即若离 · 心动", prompt: "不是朋友又不是情人，心跳加速的那段暧昧时光" },
  { title: "夏日海边", desc: "清新流行 · 电子", prompt: "夏天海边的清新流行歌，轻快带点电子" },
  { title: "打工励志", desc: "热血说唱 · 不放弃", prompt: "热血励志的说唱，打工人永不放弃" },
  { title: "梦想实现", desc: "欢呼 · 眼泪", prompt: "多年坚持终于等到的那一刻，眼泪和欢呼" },
  { title: "跨年烟花", desc: "钟声 · 愿望", prompt: "新年钟声敲响时的烟花与愿望" },
  { title: "白头偕老", desc: "长久陪伴 · 爱情", prompt: "从青春走到白发，牵着你的手到老" },
  { title: "恋爱纪念日", desc: "一年更爱一年", prompt: "从相遇那天到现在，每一年都更爱你" },
  { title: "唯美古风", desc: "中国风 · 女声吟唱", prompt: "古风中国风，唯美诗意，女声吟唱" },
  { title: "雪夜围炉", desc: "冬夜 · 温暖", prompt: "冬夜窗外飘雪，屋里煮茶的温暖" },
  { title: "考上大学", desc: "梦想照进现实", prompt: "收到录取通知书的那一刻，梦想照进现实" },
  { title: "第一份工资", desc: "成就感 · 自豪", prompt: "拿到第一份工资的成就感，小小的自豪" },
  { title: "听雨的夜", desc: "出租屋 · 治愈", prompt: "出租屋的夜，窗外雨声治愈的孤独" },
];
const musicStyleGroups = [
  { code: "genre", label: "流派", items: [
    ["Pop", "流行"], ["Rock", "摇滚"], ["Indie", "独立"], ["Alternative", "另类"], ["Electronic", "电子"], ["EDM", "EDM"], ["House", "浩室"], ["Techno", "Techno"], ["Trance", "迷幻电子"], ["Dubstep", "Dubstep"], ["Drum and Bass", "鼓打贝斯"], ["Hip-hop", "嘻哈"], ["Trap", "Trap"], ["R&B", "R&B"], ["Soul", "灵魂"], ["Funk", "放克"], ["Jazz", "爵士"], ["Bossa Nova", "波萨诺瓦"], ["Folk", "民谣"], ["Acoustic", "原声"], ["Country", "乡村"], ["Blues", "蓝调"], ["Classical", "古典"], ["Orchestral", "管弦乐"], ["Metal", "金属"], ["Punk", "朋克"], ["Reggae", "雷鬼"], ["Disco", "迪斯科"], ["Latin", "拉丁"], ["K-Pop", "K-Pop"], ["J-Pop", "J-Pop"], ["Chinese traditional", "中国风"], ["Ancient Chinese", "古风"], ["Lo-fi", "Lo-fi"], ["Synthwave", "合成波"], ["Cinematic", "电影感"], ["Ambient", "氛围"], ["Ballad", "抒情"], ["City Pop", "City Pop"], ["Anime", "动漫"], ["Game music", "游戏音乐"],
  ] },
  { code: "mood", label: "情绪", items: [
    ["Upbeat", "欢快"], ["Happy", "开心"], ["Joyful", "喜悦"], ["Sad", "悲伤"], ["Melancholy", "忧郁"], ["Heartbroken", "心碎"], ["Gentle", "温柔"], ["Tender", "柔情"], ["Passionate", "激昂"], ["Aggressive", "激烈"], ["Dreamy", "梦幻"], ["Ethereal", "空灵"], ["Chill", "舒缓"], ["Relaxing", "放松"], ["Peaceful", "平和"], ["Romantic", "浪漫"], ["Inspiring", "励志"], ["Motivational", "鼓舞"], ["Triumphant", "凯旋"], ["Nostalgic", "怀旧"], ["Dark", "黑暗"], ["Epic", "史诗"], ["Mysterious", "神秘"], ["Playful", "俏皮"], ["Groovy", "律动"], ["Energetic", "活力"], ["Hopeful", "希望"], ["Lonely", "孤独"], ["Catchy", "洗脑"], ["Warm", "温暖"],
  ] },
  { code: "instrument", label: "乐器", items: [
    ["Piano", "钢琴"], ["Acoustic guitar", "原声吉他"], ["Electric guitar", "电吉他"], ["Bass guitar", "贝斯"], ["Ukulele", "尤克里里"], ["Violin", "小提琴"], ["Cello", "大提琴"], ["Harp", "竖琴"], ["Flute", "长笛"], ["Saxophone", "萨克斯"], ["Trumpet", "小号"], ["Harmonica", "口琴"], ["Accordion", "手风琴"], ["Synth", "合成器"], ["808", "808鼓机"], ["Drums", "鼓"], ["Percussion", "打击乐"], ["Strings", "弦乐"], ["Brass", "铜管"], ["Guzheng", "古筝"], ["Erhu", "二胡"], ["Pipa", "琵琶"], ["Dizi", "笛子"], ["Xiao", "箫"], ["Suona", "唢呐"], ["Music box", "八音盒"], ["Kalimba", "拇指琴"], ["Marimba", "马林巴"], ["Gong", "锣"], ["Djembe", "非洲鼓"], ["Bagpipe", "风笛"],
  ] },
  { code: "vocal", label: "人声", items: [
    ["Male vocal", "男声"], ["Female vocal", "女声"], ["Soft vocal", "温柔人声"], ["Powerful vocal", "有力人声"], ["Breathy vocal", "气声"], ["Raspy vocal", "沙哑"], ["High-pitched vocal", "高音"], ["Deep vocal", "低音"], ["Falsetto", "假声"], ["Whisper", "耳语"], ["Rap", "说唱"], ["Chorus", "合唱"], ["Duet", "对唱"], ["Child vocal", "童声"], ["Operatic vocal", "美声"], ["Autotune", "Auto-Tune"], ["Spoken word", "朗诵"], ["Humming", "哼唱"], ["Beatbox", "Beatbox"], ["Vocal harmony", "和声"],
  ] },
  { code: "tempo", label: "速度", items: [
    ["Slow", "慢速"], ["Medium tempo", "中速"], ["Fast", "快速"], ["Very Fast", "极速"], ["Steady", "稳定节奏"], ["Changing Tempo", "变速"], ["Accelerating", "渐快"], ["Decelerating", "渐慢"], ["Rubato", "自由节奏"], ["Syncopated", "切分节奏"],
  ] },
];
const visibleMusicPacks = computed(() => showAllMusicPacks.value ? musicInspirationPacks : musicInspirationPacks.slice(0, 16));
const activeMusicStyleItems = computed(() => musicStyleGroups.find((g) => g.code === musicStyleGroup.value)?.items || []);
const latestMusicJob = computed(() => musicJobs.value[0] || null);
const foldedMusicJobs = computed(() => musicJobs.value.slice(1));
const visibleMusicJobs = computed(() => {
  if (!latestMusicJob.value) return [];
  return musicHistoryExpanded.value ? musicJobs.value : [latestMusicJob.value];
});
const playerQueue = computed<PlayerSong[]>(() => {
  const map = new Map<number, PlayerSong>();
  [...musicJobs.value, ...musicHall.value, ...userCardMusic.value].forEach((song) => {
    if (song.audioUrl) map.set(song.id, song);
  });
  posts.value.forEach((post) => {
    const music = post.music;
    if (music?.audioUrl) {
      map.set(music.id, {
        id: music.id,
        title: music.title,
        audioUrl: music.audioUrl,
        imageUrl: music.imageUrl,
        authorName: music.authorName || post.authorName,
        lyrics: music.lyrics,
        style: music.style,
        duration: music.duration,
        createdAt: post.createdAt,
        lyricist: music.lyricist,
        composer: music.composer,
      });
    }
  });
  return Array.from(map.values());
});
const currentPlayerIndex = computed(() => {
  if (!currentSong.value) return -1;
  return playerQueue.value.findIndex((song) => song.id === currentSong.value?.id);
});
const playerEffectiveDuration = computed(() => {
  if (playerDuration.value) return playerDuration.value;
  if (!currentSong.value) return 0;
  return parseMusicDuration(currentSong.value.duration);
});
const playerProgress = computed(() => {
  const duration = playerEffectiveDuration.value;
  if (!duration) return 0;
  return Math.min(100, Math.max(0, (playerCurrentTime.value / duration) * 100));
});
const playerLyricLines = computed(() => {
  const raw = currentSong.value?.lyrics || currentSong.value?.prompt || "";
  return raw
    .split(/\n+/)
    .map((v) => v.replace(/^\[[^\]]+\]\s*/, "").trim())
    .filter(Boolean);
});
function isLyricSectionTitle(line: string) {
  const v = line.trim();
  return /^(\[.*\]|【.*】|（.*）|\(.*\)|verse|chorus|pre-chorus|bridge|outro|intro|副歌|预副歌|主歌|桥段|尾声|前奏|间奏)$/i.test(v);
}

const playerSingableLyricIndexes = computed(() => {
  return playerLyricLines.value
    .map((line, index) => (isLyricSectionTitle(line) ? -1 : index))
    .filter((index) => index >= 0);
});
const playerEstimatedLyricTimings = computed(() => {
  const lines = playerLyricLines.value;
  const singable = playerSingableLyricIndexes.value;
  if (!lines.length || !singable.length || !playerDuration.value) return [];
  const intro = Math.min(18, Math.max(4, playerDuration.value * 0.08));
  const outro = Math.min(10, Math.max(3, playerDuration.value * 0.04));
  const usable = Math.max(1, playerDuration.value - intro - outro);
  const weights = singable.map((index) => {
    const text = lines[index].replace(/[，。！？、,.!?\s]/g, "");
    const charWeight = Math.max(4, text.length);
    const previousTitle = lines.slice(Math.max(0, index - 2), index).some(isLyricSectionTitle);
    return charWeight + (previousTitle ? 2 : 0);
  });
  const total = weights.reduce((sum, value) => sum + value, 0);
  let cursor = intro;
  return singable.map((lineIndex, i) => {
    const start = cursor;
    cursor += usable * (weights[i] / total);
    return { lineIndex, start, end: cursor };
  });
});
const playerLyricTimings = computed(() => {
  const raw = currentSong.value?.lyrics || "";
  return raw
    .split(/\n+/)
    .map((line) => {
      const m = line.match(/^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]/);
      if (!m) return null;
      return Number(m[1]) * 60 + Number(m[2]) + Number(`0.${m[3] || "0"}`);
    });
});
const activeLyricIndex = computed(() => {
  const lines = playerLyricLines.value;
  if (!lines.length || !playerDuration.value) return 0;
  const timings = playerLyricTimings.value;
  if (timings.some((v) => v !== null)) {
    let active = playerSingableLyricIndexes.value[0] ?? 0;
    timings.forEach((time, index) => {
      if (time !== null && playerCurrentTime.value >= time && !isLyricSectionTitle(lines[index])) active = index;
    });
    return Math.min(lines.length - 1, active);
  }
  const singable = playerSingableLyricIndexes.value;
  if (!singable.length) return Math.min(lines.length - 1, Math.floor((playerCurrentTime.value / playerDuration.value) * lines.length));
  const estimatedTimings = playerEstimatedLyricTimings.value;
  const current = playerCurrentTime.value;
  const found = estimatedTimings.find((item) => current >= item.start && current < item.end);
  if (found) return found.lineIndex;
  if (estimatedTimings.length && current < estimatedTimings[0].start) return estimatedTimings[0].lineIndex;
  return singable[singable.length - 1];
});

function scrollActiveLyric() {
  if (!playerExpanded.value) return;
  nextTick(() => {
    const box = playerLyricsRef.value;
    if (!box) return;
    const active = box.querySelector<HTMLElement>("[data-active-lyric='true']");
    if (!active) return;
    const previous = Number(box.dataset.lastActive || "-1");
    box.dataset.lastActive = String(activeLyricIndex.value);
    active.scrollIntoView({
      block: "center",
      inline: "nearest",
      behavior: Math.abs(previous - activeLyricIndex.value) > 2 ? "auto" : "smooth",
    });
  });
}

watch([activeLyricIndex, playerExpanded, currentSong], scrollActiveLyric);

function showToast(message: string) {
  toast.value = message;
  window.setTimeout(() => {
    if (toast.value === message) toast.value = "";
  }, 2200);
}

function navHot(tab: Tab) {
  return (homeData.value.navHotTabs || []).map((v) => v.toLowerCase()).includes(tab);
}

function fmtTime(value?: string) {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "-";
  return `${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function flowAmountText(flow: WalletFlow) {
  const incomeTypes = ["RECHARGE", "TASK_REWARD", "MUSIC_TIP_IN"];
  const sign = incomeTypes.includes(flow.type) ? "+" : "-";
  return `${sign}¥${flow.amount}`;
}

function flowTone(flow: WalletFlow) {
  return ["RECHARGE", "TASK_REWARD", "MUSIC_TIP_IN"].includes(flow.type) ? "income" : "expense";
}

function countdownText(value?: string) {
  if (!value) return "未设置";
  const end = new Date(value).getTime();
  if (Number.isNaN(end)) return "未设置";
  let left = end - nowTs.value;
  if (left <= 0) return "已结束";
  const days = Math.floor(left / 86400000);
  left %= 86400000;
  const hours = Math.floor(left / 3600000);
  left %= 3600000;
  const minutes = Math.floor(left / 60000);
  if (days > 0) return `${days}天${hours}小时`;
  if (hours > 0) return `${hours}小时${minutes}分`;
  return `${Math.max(1, minutes)}分钟`;
}

function isHotPost(post: PlazaPost) {
  return !!post.hot;
}

function avatarText(name?: string) {
  return (name || "匿").slice(0, 1);
}

function sliceItems<T>(value: SlicePage<T> | T[]) {
  return Array.isArray(value) ? value : value.items || [];
}

function sliceHasMore<T>(value: SlicePage<T> | T[], fallbackSize = pageSize) {
  return Array.isArray(value) ? value.length === fallbackSize : value.hasMore;
}

function isAiComment(name?: string) {
  return /\sAI$/i.test((name || "").trim());
}

function aiLogoText(ai: PlazaMeta["aiProviders"][number]) {
  const name = (ai.name || "").trim();
  const logo = (ai.logoText || "").trim();
  if (/[\u4e00-\u9fa5]/.test(name) && /^[A-Za-z]{1,4}$/.test(logo)) return name.slice(0, 1);
  return (logo || ai.abbr || name || "AI").slice(0, 2);
}

async function switchTab(tab: Tab) {
  activeTab.value = tab;
  await loadUnreadMessageCount();
  if (tab === "plaza") await Promise.all([loadHome(), loadPlazaMeta(), loadPosts(true)]);
  if (tab === "tasks") await loadTasks(true);
  if (tab === "music") await loadMusicJobs();
  if (tab === "me") {
    await loadMe();
    if (isLoggedIn) {
      await Promise.all([loadWallet(), loadFollows(), loadUnreadMessageCount()]);
      if (mePanel.value === "posts") await loadMyPosts(true);
      if (mePanel.value === "orders") await loadMyOrders(true);
      if (mePanel.value === "messages") await loadMessages();
      if (mePanel.value === "wallet") await Promise.all([loadWithdraws(), loadWalletFlows()]);
    }
  }
}

async function openMessages() {
  if (!requireLogin()) return;
  await switchTab("me");
  await switchMePanel("messages");
}

function genderText(gender?: string) {
  const map: Record<string, string> = { MALE: "男生", FEMALE: "女生", UNKNOWN: "未设置", AUTO: "自动识别" };
  return gender ? map[gender] || gender : "未设置";
}

function plazaMetaText(post: PlazaPost) {
  const parts = [fmtTime(post.createdAt)];
  if (post.gender && post.gender !== "UNKNOWN" && post.gender !== "AUTO") parts.push(genderText(post.gender));
  parts.push(post.categoryName || post.category);
  return parts.join(" · ");
}

function useMusicInspiration(pack: { title: string; prompt: string }) {
  musicForm.value.title = pack.title;
  musicForm.value.prompt = pack.prompt;
  musicForm.value.custom_mode = false;
  musicForm.value.instrumental = false;
  lastMusicLyricTheme.value = "";
  lastMusicLyricTitle.value = "";
  lastMusicLyricOutput.value = "";
}

function selectedMusicStyles() {
  return musicForm.value.style.split(",").map((v) => v.trim()).filter(Boolean);
}

function musicStyleSelected(style: string) {
  return selectedMusicStyles().map((v) => v.toLowerCase()).includes(style.toLowerCase());
}

function toggleMusicStyle(style: string) {
  const styles = selectedMusicStyles();
  const exists = styles.findIndex((v) => v.toLowerCase() === style.toLowerCase());
  if (exists >= 0) styles.splice(exists, 1);
  else styles.push(style);
  musicForm.value.style = styles.join(", ");
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: "待审核",
    PUBLISHED: "进行中",
    CLOSED: "已关闭",
    ACCEPTED: "已接单",
    SUBMITTED: "待审核",
    APPROVED: "已通过",
    REJECTED_RESUBMIT: "需重提",
    SETTLED: "已结算",
    NEW: "新反馈",
    PENDING: "审核中",
    PAID: "已打款",
    REJECTED: "已驳回",
    SUCCESS: "成功",
  };
  return status ? map[status] || status : "-";
}

function submissionStatusClass(status?: string) {
  const value = (status || "").toUpperCase();
  return {
    pending: ["SUBMITTED", "PENDING", "PENDING_AUDIT"].includes(value),
    success: ["APPROVED", "SETTLED", "SUCCESS"].includes(value),
    danger: ["REJECTED", "REJECTED_RESUBMIT", "FAILED", "ERROR"].includes(value),
  };
}

function musicStatusText(status?: string) {
  const map: Record<string, string> = {
    SUBMITTED: "生成中",
    PENDING: "排队中",
    PROCESSING: "生成中",
    RUNNING: "生成中",
    SUCCESS: "已完成",
    COMPLETED: "已完成",
    FAILED: "失败",
  };
  const key = (status || "").toUpperCase();
  return map[key] || status || "生成中";
}

function musicGenerationPercent(job: MusicJob) {
  const visual = musicGenerationVisual[job.id];
  if (Number.isFinite(visual)) return Math.round(Math.min(100, Math.max(0, visual)));
  if (musicReady(job)) return 100;
  const key = (job.status || "").toUpperCase();
  if (key === "FAILED" || key === "ERROR") return 100;
  if (key === "SUCCESS" || key === "COMPLETED" || key === "COMPLETE") return 100;
  const created = Date.parse(job.createdAt || "");
  const elapsed = Number.isFinite(created) ? Math.max(0, nowTs.value - created) : 0;
  const timePercent = Math.min(95, 18 + Math.floor(elapsed / 1800));
  if (job.imageUrl) return Math.max(86, timePercent);
  if (key === "RUNNING" || key === "PROCESSING") return Math.max(55, timePercent);
  if (key === "PENDING") return Math.max(30, Math.min(timePercent, 88));
  if (key === "SUBMITTED") return Math.max(22, Math.min(timePercent, 92));
  return Math.max(25, timePercent);
}

function musicGenerationBasePercent(job: MusicJob, apiPercent?: number) {
  if (Number.isFinite(apiPercent)) return Math.min(96, Math.max(5, Number(apiPercent)));
  const key = (job.status || "").toUpperCase();
  if (key === "FAILED" || key === "ERROR" || key === "SUCCESS" || key === "COMPLETED" || key === "COMPLETE") return 100;
  const created = Date.parse(job.createdAt || "");
  const elapsed = Number.isFinite(created) ? Math.max(0, nowTs.value - created) : 0;
  const timePercent = Math.min(95, 18 + Math.floor(elapsed / 1800));
  if (job.imageUrl) return Math.max(86, timePercent);
  if (key === "RUNNING" || key === "PROCESSING") return Math.max(55, timePercent);
  if (key === "PENDING") return Math.max(30, Math.min(timePercent, 88));
  if (key === "SUBMITTED") return Math.max(22, Math.min(timePercent, 92));
  return Math.max(25, timePercent);
}

function syncMusicGenerationProgress(job: MusicJob, apiPercent?: number) {
  const hasVisual = Object.prototype.hasOwnProperty.call(musicGenerationVisual, job.id);
  const target = musicReady(job) ? 100 : (job.audioUrl ? 96 : musicGenerationBasePercent(job, apiPercent));
  if (!hasVisual) {
    musicGenerationVisual[job.id] = musicReady(job) ? 100 : Math.min(target, 35);
  }
  musicGenerationTarget[job.id] = Math.max(musicGenerationTarget[job.id] || 0, target);
}

function tickMusicGenerationProgress() {
  [...musicJobs.value, ...musicHall.value].forEach((job) => syncMusicGenerationProgress(job));
  Object.keys(musicGenerationTarget).forEach((id) => {
    const key = Number(id);
    const current = musicGenerationVisual[key] || 0;
    const target = musicGenerationTarget[key] || 0;
    if (current >= target) return;
    const step = target >= 100 ? 5 : target - current > 12 ? 3 : 1;
    musicGenerationVisual[key] = Math.min(target, current + step);
  });
}

function musicPlayable(job: MusicJob) {
  return musicReady(job) && musicGenerationPercent(job) >= 100;
}

function musicReady(job: MusicJob) {
  return Boolean(job.audioUrl && parseMusicDuration(job.duration) > 0);
}

function musicAttachmentReady(music?: { audioUrl?: string; duration?: string }) {
  return Boolean(music?.audioUrl && parseMusicDuration(music.duration) > 0);
}

function musicAttachmentDeleted(music?: { deleted?: boolean; id?: number; audioUrl?: string }) {
  return Boolean(music?.deleted || (music && music.id === 0 && !music.audioUrl));
}

function normalizeMusicError(message?: string) {
  const raw = String(message || "").trim();
  if (!raw) return "";
  const lower = raw.toLowerCase();
  if (lower.includes("call frequency is too high") || lower.includes("too many requests") || lower.includes("rate limit") || lower.includes("frequency")) {
    return "AI音乐生成服务调用过于频繁，请稍后再试";
  }
  if (lower.includes("current credits are insufficient") || lower.includes("please top up") || lower.includes("insufficient")) {
    return "AI音乐生成服务额度不足，请联系平台管理员充值后再试";
  }
  return raw;
}

function musicGenerationLabel(job: MusicJob) {
  const key = (job.status || "").toUpperCase();
  if (key === "FAILED" || key === "ERROR") return normalizeMusicError(job.errorMessage) || "生成失败";
  if (job.audioUrl && !musicReady(job)) return "音频已生成，正在补齐时长";
  if (job.imageUrl && !job.audioUrl) return "封面已生成，音频处理中";
  return musicStatusText(job.status);
}

function musicStyleTags(song: MusicJob, max = 4) {
  return (song.style || "")
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean)
    .slice(0, max);
}

function parseMusicDuration(value?: string) {
  const raw = String(value || "").trim();
  if (!raw) return 0;
  if (/^\d+(\.\d+)?$/.test(raw)) return Number(raw);
  const parts = raw.split(":").map((v) => Number(v));
  if (parts.some((v) => !Number.isFinite(v))) return 0;
  if (parts.length === 2) return parts[0] * 60 + parts[1];
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
  const match = raw.match(/(\d+(\.\d+)?)/);
  return match ? Number(match[1]) : 0;
}

function musicTotalDuration(job: MusicJob) {
  if (currentSong.value?.id === job.id && playerEffectiveDuration.value) return playerEffectiveDuration.value;
  return parseMusicDuration(job.duration);
}

function musicCardProgress(job: MusicJob) {
  if (currentSong.value?.id === job.id) return playerProgress.value;
  return musicPlayable(job) ? 0 : musicGenerationPercent(job);
}

function musicCardElapsed(job: MusicJob) {
  if (currentSong.value?.id === job.id) return fmtDuration(playerCurrentTime.value);
  return "0:00";
}

function musicCardRemaining(job: MusicJob) {
  if (currentSong.value?.id === job.id && playerEffectiveDuration.value) {
    return "-" + fmtDuration(Math.max(0, playerEffectiveDuration.value - playerCurrentTime.value));
  }
  return musicTotalDuration(job) ? fmtDuration(musicTotalDuration(job)) : "--:--";
}

async function seekMusicCard(job: MusicJob, event: Event) {
  if (!job.audioUrl) return;
  const value = Number((event.target as HTMLInputElement).value);
  if (!Number.isFinite(value)) return;
  if (currentSong.value?.id !== job.id) {
    playSong(job);
    await nextTick();
  }
  const audio = playerAudio.value;
  if (!audio) return;
  const duration = playerDuration.value || musicTotalDuration(job);
  audio.currentTime = duration ? (value / 100) * duration : 0;
  playerCurrentTime.value = audio.currentTime;
}

function playButtonLabel(song: PlayerSong) {
  return currentSong.value?.id === song.id && playerPlaying.value ? "播放中" : "播放";
}

function isSongPlaying(song?: { id?: number }) {
  return Boolean(song?.id && currentSong.value?.id === song.id && playerPlaying.value);
}

function playSong(song: PlayerSong, restart = false) {
  if (!song.audioUrl) {
    showToast("这首歌还没有可播放音频");
    return;
  }
  if (currentSong.value?.id === song.id) {
    if (!playerOpen.value) playerOpen.value = true;
    if (restart) {
      const audio = playerAudio.value;
      if (!audio) return;
      audio.currentTime = 0;
      playerCurrentTime.value = 0;
      audio.play().then(() => {
        playerPlaying.value = true;
      }).catch(() => showToast("播放失败，请稍后重试"));
      return;
    }
    togglePlayerPlay();
    return;
  }
  currentSong.value = song;
  playerOpen.value = true;
  playerCurrentTime.value = 0;
  playerDuration.value = parseMusicDuration(song.duration);
  nextTick(() => {
    const audio = playerAudio.value;
    if (!audio) return;
    audio.src = fileUrl(song.audioUrl);
    audio.play().then(() => {
      playerPlaying.value = true;
    }).catch(() => {
      playerPlaying.value = false;
      showToast("浏览器阻止了自动播放，请再点一次播放");
    });
  });
}

function onPlayerMeta() {
  const audio = playerAudio.value;
  playerDuration.value = audio && Number.isFinite(audio.duration) ? audio.duration : playerEffectiveDuration.value;
}

function onPlayerTime() {
  const audio = playerAudio.value;
  playerCurrentTime.value = audio ? audio.currentTime : 0;
  if (audio && Number.isFinite(audio.duration)) playerDuration.value = audio.duration;
  else if (!playerDuration.value && playerEffectiveDuration.value) playerDuration.value = playerEffectiveDuration.value;
}

function seekPlayer(event: Event) {
  const audio = playerAudio.value;
  if (!audio) return;
  const value = Number((event.target as HTMLInputElement).value);
  if (!Number.isFinite(value)) return;
  audio.currentTime = value;
  playerCurrentTime.value = value;
}

function fmtDuration(seconds: number) {
  if (!Number.isFinite(seconds) || seconds <= 0) return "0:00";
  const s = Math.floor(seconds);
  const m = Math.floor(s / 60);
  return `${m}:${String(s % 60).padStart(2, "0")}`;
}

function togglePlayerPlay() {
  const audio = playerAudio.value;
  if (!audio || !currentSong.value) return;
  if (audio.paused) {
    audio.play().then(() => {
      playerPlaying.value = true;
    }).catch(() => showToast("播放失败，请稍后重试"));
  } else {
    audio.pause();
    playerPlaying.value = false;
  }
}

function playAdjacentSong(step: number, restartSame = false) {
  const queue = playerQueue.value;
  if (!queue.length) return;
  const idx = currentPlayerIndex.value >= 0 ? currentPlayerIndex.value : 0;
  const next = queue[(idx + step + queue.length) % queue.length];
  playSong(next, restartSame && next.id === currentSong.value?.id);
}

function onPlayerEnded() {
  playerCurrentTime.value = 0;
  playAdjacentSong(1, true);
}

function closePlayer() {
  playerExpanded.value = false;
  playerListOpen.value = false;
  playerOpen.value = false;
  playerAudio.value?.pause();
  playerPlaying.value = false;
}

function useSongAsTemplate(song: MusicJob) {
  musicForm.value.title = song.title ? `${song.title} 同款` : musicForm.value.title;
  musicForm.value.prompt = song.lyrics || song.prompt || musicForm.value.prompt;
  musicForm.value.style = song.style || musicForm.value.style;
  musicForm.value.instrumental = song.instrumental;
  musicForm.value.custom_mode = song.customMode;
  musicForm.value.lang = song.lang || musicForm.value.lang;
  lastMusicLyricTheme.value = "";
  lastMusicLyricTitle.value = "";
  lastMusicLyricOutput.value = "";
  activeTab.value = "music";
  window.scrollTo({ top: 0, behavior: "smooth" });
  showToast("已填入同款创作参数");
}

async function copyMusicStyle(song: MusicJob) {
  const text = song.style || "";
  if (!text) return showToast("暂无风格可复制");
  if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(text);
  showToast("风格已复制");
}

function downloadLyrics(song: MusicJob) {
  const text = song.lyrics || song.prompt || "";
  if (!text) return showToast("暂无歌词可下载");
  const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${song.title || "lyrics"}.txt`;
  a.click();
  URL.revokeObjectURL(url);
}

function downloadCover(song: MusicJob) {
  if (!song.imageUrl) return showToast("暂无封面可下载");
  const a = document.createElement("a");
  a.href = fileUrl(song.imageUrl);
  a.target = "_blank";
  a.download = `${song.title || "cover"}.jpg`;
  a.click();
}

async function renameMusic(job: MusicJob) {
  if (!requireLogin()) return;
  const title = window.prompt("请输入新的歌曲名称", job.title || "");
  if (!title?.trim()) return;
  await runBusy(async () => {
    const fresh = await api<MusicJob>(`/api/mini/music/song/${job.id}`, { method: "PATCH", body: { title: title.trim() } });
    musicJobs.value = musicJobs.value.map((v) => (v.id === fresh.id ? fresh : v));
    musicHall.value = musicHall.value.map((v) => (v.id === fresh.id ? fresh : v));
    if (currentSong.value?.id === fresh.id) currentSong.value = { ...currentSong.value, title: fresh.title };
    showToast("已改名");
  });
}

async function deleteMusic(job: MusicJob) {
  if (!requireLogin()) return;
  if (!window.confirm(`确认删除《${job.title || "这首歌"}》吗？`)) return;
  await runBusy(async () => {
    await api(`/api/mini/music/song/${job.id}`, { method: "DELETE" });
    musicJobs.value = musicJobs.value.filter((v) => v.id !== job.id);
    musicHall.value = musicHall.value.filter((v) => v.id !== job.id);
    if (currentSong.value?.id === job.id) closePlayer();
    showToast("已删除");
  });
}

function todoMusicFeature() {
  showToast("功能开发中");
}

function requireLogin() {
  if (isLoggedIn.value) return true;
  loginOpen.value = true;
  return false;
}

function canLoadMore() {
  if (loading.value || paging.value || refreshing.value) return false;
  if (activeTab.value === "plaza") return postHasMore.value;
  if (activeTab.value === "tasks") return taskHasMore.value;
  if (activeTab.value === "me" && mePanel.value === "posts") return myPostHasMore.value;
  if (activeTab.value === "me" && mePanel.value === "orders") return orderHasMore.value;
  return false;
}

async function runBusy(fn: () => Promise<void>) {
  loading.value = true;
  try {
    await fn();
  } catch (e) {
    if (e instanceof Error && e.message === "AUTH") {
      clearToken();
      token.value = "";
      me.value = null;
      loginOpen.value = true;
    } else {
      showToast(e instanceof Error ? e.message : "操作失败");
    }
  } finally {
    loading.value = false;
  }
}

async function loadNextPage() {
  if (!canLoadMore()) return;
  paging.value = true;
  try {
    if (activeTab.value === "plaza") await loadPosts();
    if (activeTab.value === "tasks") await loadTasks();
    if (activeTab.value === "music") await loadMusicJobs();
    if (activeTab.value === "me" && mePanel.value === "posts") await loadMyPosts();
    if (activeTab.value === "me" && mePanel.value === "orders") await loadMyOrders();
  } finally {
    paging.value = false;
  }
}

async function refreshCurrentView() {
  if (refreshing.value || loading.value) return;
  refreshing.value = true;
  try {
    if (activeTab.value === "plaza") {
      await Promise.all([loadHome(), loadPlazaMeta(), loadPosts(true)]);
    } else if (activeTab.value === "tasks") {
      await loadTasks(true);
    } else if (activeTab.value === "music") {
      await loadMusicJobs();
    } else if (activeTab.value === "me") {
      await Promise.all([loadMe(), loadWallet(), loadFollows()]);
      if (mePanel.value === "posts") await loadMyPosts(true);
      if (mePanel.value === "orders") await loadMyOrders(true);
      if (mePanel.value === "messages") await loadMessages();
      if (mePanel.value === "wallet") await Promise.all([loadWithdraws(), loadWalletFlows()]);
    }
    showToast("已刷新");
  } finally {
    refreshing.value = false;
  }
}

function onWindowScroll() {
  const doc = document.documentElement;
  const distanceToBottom = doc.scrollHeight - window.innerHeight - window.scrollY;
  if (distanceToBottom < 280) void loadNextPage();
}

function onTouchStart(e: TouchEvent) {
  touchStartY = e.touches[0]?.clientY || 0;
  touchStartAtTop = window.scrollY <= 4;
}

function onTouchEnd(e: TouchEvent) {
  if (!touchStartAtTop) return;
  const endY = e.changedTouches[0]?.clientY || touchStartY;
  const delta = endY - touchStartY;
  if (delta > 90) void refreshCurrentView();
}

async function loadMe() {
  if (!token.value) return;
  try {
    me.value = await api<User>("/api/mp/auth/me");
    profileForm.value = {
      nickname: me.value.nickname || "",
      avatar: me.value.avatar || "",
      gender: me.value.gender || "UNKNOWN",
      signature: me.value.signature || "",
    };
  } catch {
    token.value = "";
    me.value = null;
    clearToken();
  }
}

async function loadHome() {
  homeData.value = await api<HomeData>("/api/mp/home");
}

async function loadPlazaMeta() {
  plazaMeta.value = await api("/api/mp/public/plaza/meta");
}

async function loadPosts(reset = false) {
  if (reset) {
    postPage.value = 1;
    postHasMore.value = true;
  }
  if (!postHasMore.value) return;
  await runBusy(async () => {
    const gender = postFilter.value === "MALE" || postFilter.value === "FEMALE" ? postFilter.value : "ALL";
    const category = postFilter.value === "ALL" || gender !== "ALL" ? "ALL" : postFilter.value;
    const page = await api<SlicePage<PlazaPost>>(
      `/api/mp/public/plaza/posts?sort=${postSort.value}&gender=${gender}&category=${category}&page=${postPage.value}&size=${pageSize}`,
    );
    const list = sliceItems(page);
    posts.value = reset ? list : [...posts.value, ...list];
    postHasMore.value = sliceHasMore(page);
    postPage.value += 1;
  });
}

async function loadTasks(reset = false) {
  if (reset) {
    taskPage.value = 1;
    taskHasMore.value = true;
  }
  if (!taskHasMore.value) return;
  await runBusy(async () => {
    const page = await api<SlicePage<TaskItem>>(
      `/api/mp/tasks?page=${taskPage.value}&size=${pageSize}&q=${encodeURIComponent(taskQuery.value)}&sort=${taskSort.value}`,
    );
    const list = sliceItems(page);
    tasks.value = reset ? list : [...tasks.value, ...list];
    taskHasMore.value = sliceHasMore(page);
    taskPage.value += 1;
  });
}

async function loadMusicJobs() {
  if (!requireLogin()) return;
  await runBusy(async () => {
    const [credits, packages, mine, hall] = await Promise.all([
      api<MusicCredits>("/api/mini/music/credits"),
      api<MusicPackage[]>("/api/mini/music/packages"),
      api<MusicJob[]>("/api/mini/music/my-songs"),
      api<SlicePage<MusicJob>>(`/api/mini/music/hall?sort=${musicHallSort.value}&page=1&size=${pageSize}`),
    ]);
    musicCredits.value = credits;
    musicPackages.value = packages;
    musicJobs.value = mine;
    musicHall.value = sliceItems(hall);
    musicLoaded.value = true;
    [...mine, ...musicHall.value].forEach((job) => syncMusicGenerationProgress(job));
  });
}

async function generateMusic() {
  if (!requireLogin()) return;
  if (musicSubmitting.value) return showToast("音乐正在提交生成，请勿重复点击");
  if (!musicForm.value.title.trim()) return showToast("请输入歌曲标题");
  if (!musicForm.value.prompt.trim()) return showToast("请输入歌词或创作描述");
  musicSubmitting.value = true;
  try {
    await runBusy(async () => {
      let resp: { generation_id: number };
      try {
        resp = await api<{ generation_id: number }>("/api/mini/music/generate", {
          method: "POST",
          body: {
            title: musicForm.value.title,
            prompt: musicForm.value.prompt,
            style: musicForm.value.style,
            custom_mode: musicForm.value.custom_mode,
            instrumental: musicForm.value.instrumental,
            lang: musicForm.value.lang,
          },
        });
      } catch (e) {
        const msg = e instanceof Error ? e.message : "";
        if (msg.includes("Insufficient balance")) {
          rechargeAmount.value = String(musicCredits.value?.paidPrice || "1.20");
          rechargeAfterMusic.value = true;
          await loadRechargeConfig();
          rechargeOpen.value = true;
          showToast("余额不足，请先充值");
          return;
        }
        throw e;
      }
      const job = await api<MusicJob>(`/api/mini/music/song/${resp.generation_id}`);
      musicGenerationVisual[job.id] = 8;
      syncMusicGenerationProgress(job, 18);
      musicJobs.value = [job, ...musicJobs.value.filter((v) => v.id !== job.id)];
      musicCredits.value = await api<MusicCredits>("/api/mini/music/credits");
      musicLoaded.value = true;
      showToast(job.status === "FAILED" ? "已提交但生成失败，请查看错误" : "已提交音乐生成");
    });
  } finally {
    musicSubmitting.value = false;
  }
}

function openPackagePurchase(pkg: MusicPackage) {
  if (!requireLogin()) return;
  selectedMusicPackage.value = pkg;
  packagePurchaseOpen.value = true;
}

async function buyMusicPackage() {
  if (!requireLogin() || !selectedMusicPackage.value) return;
  await runBusy(async () => {
    try {
      await api(`/api/mini/music/packages/${selectedMusicPackage.value?.code}/buy`, { method: "POST" });
    } catch (e) {
      const msg = e instanceof Error ? e.message : "";
      if (msg.includes("Insufficient balance")) {
        rechargeAmount.value = String(selectedMusicPackage.value?.price || "9.90");
        rechargeAfterPackage.value = true;
        await loadRechargeConfig();
        rechargeOpen.value = true;
        showToast("余额不足，请先充值");
        return;
      }
      throw e;
    }
    packagePurchaseOpen.value = false;
    showToast("购买成功");
    await Promise.all([loadMusicJobs(), loadWallet(), loadWalletFlows()]);
  });
}

async function assistMusic() {
  if (!requireLogin()) return;
  const currentPrompt = musicForm.value.prompt.trim();
  const currentTitle = musicForm.value.title.trim();
  const hasGeneratedLyrics = Boolean(lastMusicLyricOutput.value && currentPrompt === lastMusicLyricOutput.value);
  const hasLyricsInBox = currentPrompt.length > 20 && (musicForm.value.custom_mode || hasGeneratedLyrics || Boolean(lastMusicLyricTheme.value));
  const isRegenerate = Boolean(hasLyricsInBox || lastMusicLyricTheme.value);
  if (isRegenerate && !window.confirm("再次生成会替换为一版全新歌词，是否继续？")) return;
  const titleChanged = Boolean(lastMusicLyricTitle.value && currentTitle && currentTitle !== lastMusicLyricTitle.value);
  const originalTheme = lastMusicLyricTheme.value || currentPrompt || currentTitle;
  const theme = titleChanged
    ? [currentTitle, originalTheme && originalTheme !== currentTitle ? originalTheme : ""].filter(Boolean).join("：")
    : originalTheme;
  if (!theme) return showToast("请先填写歌曲标题或描述，再让 AI 作词");
  await runBusy(async () => {
    const res = await api<{ lyrics: string }>("/api/mini/music/generate-lyrics", {
      method: "POST",
      body: {
        title: currentTitle,
        theme,
        style: musicForm.value.style,
        previousLyrics: hasLyricsInBox ? currentPrompt : "",
      },
    });
    lastMusicLyricTheme.value = theme;
    lastMusicLyricTitle.value = currentTitle;
    lastMusicLyricOutput.value = res.lyrics.trim();
    musicForm.value.prompt = res.lyrics;
    musicForm.value.custom_mode = true;
    musicForm.value.instrumental = false;
    showToast("歌词已更新");
  });
}

async function refreshMusicJob(job: MusicJob) {
  if (!requireLogin()) return;
  await runBusy(async () => {
    const fresh = await api<MusicJob>(`/api/mini/music/song/${job.id}`);
    syncMusicGenerationProgress(fresh);
    musicJobs.value = musicJobs.value.map((v) => (v.id === fresh.id ? fresh : v));
  });
}

async function pollPendingMusicJobs() {
  if (musicPollBusy) return;
  const needsMusicRefresh = (job: MusicJob) => {
    const status = (job.status || "").toUpperCase();
    if (["FAILED", "ERROR"].includes(status)) return false;
    return !musicReady(job) || (!job.instrumental && !job.lyrics);
  };
  if (!token.value || !musicJobs.value.some(needsMusicRefresh)) return;
  musicPollBusy = true;
  try {
    const pending = musicJobs.value.filter(needsMusicRefresh).slice(0, 3);
    await Promise.allSettled(pending.map(async (job) => {
      const fresh = await api<MusicJob>(`/api/mini/music/song/${job.id}`);
      syncMusicGenerationProgress(fresh);
      musicJobs.value = musicJobs.value.map((v) => (v.id === fresh.id ? fresh : v));
      if (currentSong.value?.id === fresh.id) currentSong.value = { ...currentSong.value, ...fresh };
    }));
  } finally {
    musicPollBusy = false;
  }
}

async function publishMusic(job: MusicJob) {
  if (!requireLogin()) return;
  await runBusy(async () => {
    const fresh = await api<MusicJob>(`/api/mini/music/song/${job.id}/publish`, { method: "POST" });
    musicJobs.value = musicJobs.value.map((v) => (v.id === fresh.id ? fresh : v));
    const hall = await api<SlicePage<MusicJob>>(`/api/mini/music/hall?sort=${musicHallSort.value}&page=1&size=${pageSize}`);
    musicHall.value = sliceItems(hall);
    showToast("已发布到音乐广场");
  });
}

async function shareMusic(job: MusicJob) {
  if (!requireLogin()) return;
  await runBusy(async () => {
    const fresh = await api<MusicJob>(`/api/mini/music/song/${job.id}/share-circle`, { method: "POST" });
    musicJobs.value = musicJobs.value.map((v) => (v.id === fresh.id ? fresh : v));
    showToast("已同步到广场");
  });
}

async function rateMusic(job: MusicJob, stars = 5) {
  if (!requireLogin()) return;
  await runBusy(async () => {
    const fresh = await api<MusicJob>(`/api/mini/music/song/${job.id}/rate`, { method: "POST", body: { stars } });
    musicJobs.value = musicJobs.value.map((v) => (v.id === fresh.id ? fresh : v));
    musicHall.value = musicHall.value.map((v) => (v.id === fresh.id ? fresh : v));
    showToast(fresh.liked ? "已点赞" : "已取消点赞");
  });
}

async function loadWallet() {
  if (!token.value) return;
  try {
    wallet.value = await api<Wallet>("/api/mp/wallet");
  } catch {
    wallet.value = null;
  }
}

async function loadWalletFlows() {
  if (!token.value) return;
  try {
    walletFlows.value = await api<WalletFlow[]>("/api/mp/wallet/flows");
  } catch {
    walletFlows.value = [];
  }
}

async function loadRechargeConfig() {
  rechargeConfig.value = await api<RechargeConfig>("/api/mp/wallet/recharge-config");
}

async function loadMyPosts(reset = false) {
  if (!requireLogin()) return;
  if (reset) {
    myPostPage.value = 1;
    myPostHasMore.value = true;
  }
  if (!myPostHasMore.value) return;
  await runBusy(async () => {
    const page = await api<SlicePage<PlazaPost>>(`/api/mp/plaza/my-posts?page=${myPostPage.value}&size=${pageSize}`);
    const list = sliceItems(page);
    myPosts.value = reset ? list : [...myPosts.value, ...list];
    myPostHasMore.value = sliceHasMore(page);
    myPostPage.value += 1;
  });
}

async function loadMyOrders(reset = false) {
  if (!requireLogin()) return;
  if (reset) {
    orderPage.value = 1;
    orderHasMore.value = true;
  }
  if (!orderHasMore.value) return;
  await runBusy(async () => {
    const page = await api<SlicePage<OrderItem>>(`/api/mp/orders/my?page=${orderPage.value}&size=${pageSize}`);
    const list = sliceItems(page);
    myOrders.value = reset ? list : [...myOrders.value, ...list];
    orderHasMore.value = sliceHasMore(page);
    orderPage.value += 1;
  });
}

async function loadMessages() {
  if (!requireLogin()) return;
  await runBusy(async () => {
    messages.value = await api<MessageItem[]>("/api/mp/messages");
    unreadMessageCount.value = messages.value.filter((m) => !m.read).length;
  });
}

async function markMessageRead(item: MessageItem) {
  if (item.read) return;
  await api(`/api/mp/messages/${item.id}/read`, { method: "POST" });
  item.read = true;
  unreadMessageCount.value = Math.max(0, unreadMessageCount.value - 1);
}

async function loadUnreadMessageCount() {
  if (!isLoggedIn.value) {
    unreadMessageCount.value = 0;
    return;
  }
  try {
    const r = await api<{ count: number }>("/api/mp/messages/unread-count");
    unreadMessageCount.value = Number(r.count || 0);
  } catch {
    unreadMessageCount.value = 0;
  }
}

async function markAllMessagesRead() {
  if (!requireLogin()) return;
  await runBusy(async () => {
    await api("/api/mp/messages/read-all", { method: "POST" });
    messages.value = messages.value.map((m) => ({ ...m, read: true }));
    unreadMessageCount.value = 0;
  });
}

async function loadWithdraws() {
  if (!requireLogin()) return;
  await runBusy(async () => {
    withdraws.value = await api<WithdrawItem[]>("/api/mp/wallet/my-withdraws");
    walletFlows.value = await api<WalletFlow[]>("/api/mp/wallet/flows");
  });
}

async function loadFollows() {
  if (!token.value) return;
  await runBusy(async () => {
    const [summary, followingList, followerList] = await Promise.all([
      api<FollowSummary>("/api/mp/plaza/follows/summary"),
      api<SlicePage<FollowUser>>("/api/mp/plaza/follows/following?page=1&size=20"),
      api<SlicePage<FollowUser>>("/api/mp/plaza/follows/followers?page=1&size=20"),
    ]);
    followSummary.value = summary;
    following.value = sliceItems(followingList);
    followers.value = sliceItems(followerList);
  });
}

async function openUserCard(userId: number) {
  userCardOpen.value = true;
  userCardView.value = "home";
  userCardContentTab.value = "posts";
  userCardDetailPost.value = null;
  userCardDetailComments.value = [];
  userCardCommentText.value = "";
  await runBusy(async () => {
    const [card, list, taskList, musicList] = await Promise.all([
      api<UserCard>(`/api/mp/public/plaza/users/${userId}/card`),
      api<SlicePage<PlazaPost>>(`/api/mp/public/plaza/users/${userId}/posts?page=1&size=6`),
      api<SlicePage<TaskItem>>(`/api/mp/public/plaza/users/${userId}/tasks?page=1&size=6`),
      api<SlicePage<MusicJob>>(`/api/mp/public/plaza/users/${userId}/music?page=1&size=6`),
    ]);
    userCard.value = card;
    userCardPosts.value = sliceItems(list);
    userCardTasks.value = sliceItems(taskList);
    userCardMusic.value = sliceItems(musicList);
  });
}

async function openUserCardPost(post: PlazaPost) {
  userCardDetailPost.value = post;
  userCardView.value = "post";
  userCardCommentText.value = "";
  await runBusy(async () => {
    userCardDetailComments.value = await api<CommentItem[]>(`/api/mp/public/plaza/posts/${post.id}/comments`);
  });
}

async function toggleUserPostComments(post: PlazaPost) {
  userPostCommentsOpen[post.id] = !userPostCommentsOpen[post.id];
  if (userPostCommentsOpen[post.id] && !userPostComments[post.id]) {
    await runBusy(async () => {
      userPostComments[post.id] = await api<CommentItem[]>(`/api/mp/public/plaza/posts/${post.id}/comments`);
    });
  }
}

async function submitUserPostComment(post: PlazaPost) {
  if (!requireLogin()) return;
  const content = (userPostCommentText[post.id] || "").trim();
  if (!content) return;
  await runBusy(async () => {
    await api(`/api/mp/plaza/posts/${post.id}/comments`, { method: "POST", body: { content } });
    userPostCommentText[post.id] = "";
    post.commentCount += 1;
    userPostComments[post.id] = await api<CommentItem[]>(`/api/mp/public/plaza/posts/${post.id}/comments`);
    userPostCommentsOpen[post.id] = true;
  });
}

async function likeUserMusic(song: MusicJob) {
  if (!requireLogin()) return;
  await runBusy(async () => {
    const fresh = await api<MusicJob>(`/api/mini/music/song/${song.id}/rate`, { method: "POST", body: { stars: 5 } });
    userCardMusic.value = userCardMusic.value.map((v) => (v.id === fresh.id ? fresh : v));
    musicHall.value = musicHall.value.map((v) => (v.id === fresh.id ? fresh : v));
    showToast(fresh.liked ? "已点赞" : "已取消点赞");
  });
}

function openWithdrawDetail(item: WithdrawItem) {
  withdrawDetail.value = item;
  withdrawDetailOpen.value = true;
}

async function toggleUserMusicComments(song: MusicJob) {
  userMusicCommentsOpen[song.id] = !userMusicCommentsOpen[song.id];
  if (userMusicCommentsOpen[song.id] && !userMusicComments[song.id]) {
    await runBusy(async () => {
      userMusicComments[song.id] = await api<CommentItem[]>(`/api/mini/music/song/${song.id}/comments`);
    });
  }
}

async function submitUserMusicComment(song: MusicJob) {
  if (!requireLogin()) return;
  const content = (userMusicCommentText[song.id] || "").trim();
  if (!content) return;
  await runBusy(async () => {
    await api(`/api/mini/music/song/${song.id}/comments`, { method: "POST", body: { content } });
    userMusicCommentText[song.id] = "";
    userMusicComments[song.id] = await api<CommentItem[]>(`/api/mini/music/song/${song.id}/comments`);
    userMusicCommentsOpen[song.id] = true;
  });
}

async function shareUserMusic(song: MusicJob) {
  const url = `${window.location.origin}${window.location.pathname}#music-${song.id}`;
  const title = `AI音乐《${song.title || "未命名"}》`;
  const text = song.prompt || "分享了一首AI音乐";
  try {
    if (navigator.share) {
      await navigator.share({ title, text, url });
      return;
    }
    await navigator.clipboard.writeText(`${title}\n${text}\n${url}`);
    showToast("链接已复制");
  } catch {
    showToast("分享已取消");
  }
}

async function submitUserCardComment() {
  if (!requireLogin() || !userCardDetailPost.value) return;
  if (!userCardCommentText.value.trim()) return;
  await runBusy(async () => {
    await api(`/api/mp/plaza/posts/${userCardDetailPost.value?.id}/comments`, {
      method: "POST",
      body: { content: userCardCommentText.value },
    });
    userCardCommentText.value = "";
    if (userCardDetailPost.value) {
      userCardDetailPost.value.commentCount += 1;
      userCardDetailComments.value = await api<CommentItem[]>(`/api/mp/public/plaza/posts/${userCardDetailPost.value.id}/comments`);
    }
  });
}

function openComposer() {
  if (!requireLogin()) return;
  composerOpen.value = true;
}

function openPublishTask() {
  if (!requireLogin()) return;
  publishOpen.value = true;
}

function openFeedback() {
  if (!requireLogin()) return;
  feedbackOpen.value = true;
}

async function toggleFollowUser(card = userCard.value) {
  if (!requireLogin() || !card || card.self) return;
  await runBusy(async () => {
    if (card.followed) {
      await api(`/api/mp/plaza/follow/${card.userId}`, { method: "DELETE" });
      card.followed = false;
      card.followerCount = Math.max(0, card.followerCount - 1);
    } else {
      await api(`/api/mp/plaza/follow/${card.userId}`, { method: "POST" });
      card.followed = true;
      card.followerCount += 1;
    }
    posts.value.forEach((p) => {
      if (p.authorId === card.userId) p.followed = card.followed;
    });
    await loadFollows();
  });
}

async function toggleFollowPost(post: PlazaPost) {
  if (!requireLogin()) return;
  await runBusy(async () => {
    if (post.followed) {
      await api(`/api/mp/plaza/follow/${post.authorId}`, { method: "DELETE" });
      post.followed = false;
    } else {
      await api(`/api/mp/plaza/follow/${post.authorId}`, { method: "POST" });
      post.followed = true;
    }
    if (userCard.value?.userId === post.authorId) {
      userCard.value.followed = post.followed;
    }
    await loadFollows();
  });
}

async function switchMePanel(panel: MePanel) {
  if (panel !== "overview" && !requireLogin()) return;
  mePanel.value = panel;
  if (panel === "posts" && !myPosts.value.length) await loadMyPosts(true);
  if (panel === "orders" && !myOrders.value.length) await loadMyOrders(true);
  if (panel === "messages") await loadMessages();
  if (panel === "wallet") {
    await loadWallet();
    if (!withdraws.value.length) await loadWithdraws();
    if (!walletFlows.value.length) await loadWalletFlows();
  }
  if (panel === "follows" && !followSummary.value) await loadFollows();
}

function validEmail() {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim());
}

async function sendEmailCode() {
  if (!validEmail()) return showToast("请输入正确邮箱");
  if (emailCodeLeft.value > 0) return;
  await runBusy(async () => {
    await api<void>("/api/mp/auth/email/send-code", { method: "POST", body: { email: email.value.trim() } });
    emailCodeExpireAt.value = Date.now() + 60_000;
    nowTs.value = Date.now();
    showToast("验证码已发送");
  });
}

async function isEmailRegistered() {
  const status = await api<{ registered: boolean }>("/api/mp/auth/email/status", {
    method: "POST",
    body: { email: email.value.trim() },
  });
  return status.registered;
}

async function finishLogin(res: { token: string; user: User }) {
  setToken(res.token);
  token.value = res.token;
  me.value = res.user;
  loginOpen.value = false;
  code.value = "";
  password.value = "";
  passwordConfirm.value = "";
  registerStep.value = "none";
  showToast("登录成功");
  await Promise.all([loadWallet(), loadPosts(true), loadTasks(true), loadUnreadMessageCount()]);
}

async function emailPasswordLogin() {
  if (!validEmail()) return showToast("请输入正确邮箱");
  if (!password.value.trim()) return showToast("请输入密码");
  await runBusy(async () => {
    if (!(await isEmailRegistered())) {
      registerStep.value = "password";
      passwordConfirm.value = "";
      showToast("该邮箱未注册，请设置密码完成注册");
      return;
    }
    const res = await api<{ token: string; user: User }>("/api/mp/auth/email-password-login", {
      method: "POST",
      body: { email: email.value.trim(), password: password.value },
    });
    await finishLogin(res);
  });
}

async function emailCodeLogin() {
  if (!validEmail()) return showToast("请输入正确邮箱");
  if (!code.value.trim()) return showToast("请输入验证码");
  await runBusy(async () => {
    if (!(await isEmailRegistered())) {
      await api<void>("/api/mp/auth/email/verify-code", {
        method: "POST",
        body: { email: email.value.trim(), code: code.value },
      });
      registerStep.value = "password";
      password.value = "";
      passwordConfirm.value = "";
      showToast("该邮箱未注册，请设置密码完成注册");
      return;
    }
    const res = await api<{ token: string; user: User }>("/api/mp/auth/email-code-login", {
      method: "POST",
      body: { email: email.value.trim(), code: code.value },
    });
    await finishLogin(res);
  });
}

async function emailRegister() {
  if (!validEmail()) return showToast("请输入正确邮箱");
  if (!code.value.trim()) return showToast("请输入验证码");
  if (password.value.length < 6 || password.value.length > 32) return showToast("密码长度需为6-32位");
  if (password.value !== passwordConfirm.value) return showToast("两次密码不一致");
  await runBusy(async () => {
    const res = await api<{ token: string; user: User }>("/api/mp/auth/email-register", {
      method: "POST",
      body: { email: email.value.trim(), code: code.value, password: password.value },
    });
    await finishLogin(res);
  });
}

function logout() {
  clearToken();
  token.value = "";
  me.value = null;
  wallet.value = null;
  walletFlows.value = [];
  musicJobs.value = [];
  musicCredits.value = null;
  musicPackages.value = [];
  musicHall.value = [];
  musicLoaded.value = false;
  myPosts.value = [];
  myOrders.value = [];
  messages.value = [];
  unreadMessageCount.value = 0;
  withdraws.value = [];
  followSummary.value = null;
  showToast("已退出登录");
}

async function onProfileAvatar(e: Event) {
  if (!requireLogin()) return;
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  await runBusy(async () => {
    profileForm.value.avatar = await upload(file);
  });
}

async function saveProfile() {
  if (!requireLogin()) return;
  await runBusy(async () => {
    me.value = await api<User>("/api/mp/auth/me", { method: "POST", body: profileForm.value });
    editProfileOpen.value = false;
    showToast("资料已保存");
    await loadPosts(true);
  });
}

async function submitFeedback() {
  if (!requireLogin()) return;
  if (!feedbackForm.value.content.trim()) return showToast("请填写反馈内容");
  await runBusy(async () => {
    await api("/api/mp/feedbacks", { method: "POST", body: feedbackForm.value });
    feedbackForm.value = { content: "", contact: "" };
    feedbackOpen.value = false;
    showToast("反馈已提交");
  });
}

async function onWithdrawQr(e: Event) {
  if (!requireLogin()) return;
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  await runBusy(async () => {
    withdrawForm.value.qrCodeUrl = await upload(file);
    await api("/api/mp/wallet/withdraw-qr", { method: "POST", body: { qrCodeUrl: withdrawForm.value.qrCodeUrl } });
    await loadWallet();
  });
}

async function applyWithdraw() {
  if (!requireLogin()) return;
  if (!withdrawForm.value.amount) return showToast("请输入提现金额");
  if (!withdrawForm.value.qrCodeUrl && !wallet.value?.withdrawQrCodeUrl) return showToast("请先上传收款码");
  await runBusy(async () => {
    await api("/api/mp/wallet/withdraw", {
      method: "POST",
      body: {
        amount: withdrawForm.value.amount,
        channel: "WECHAT",
        qrCodeUrl: withdrawForm.value.qrCodeUrl || wallet.value?.withdrawQrCodeUrl,
      },
    });
    withdrawForm.value.amount = "";
    withdrawOpen.value = false;
    showToast("提现申请已提交");
    await Promise.all([loadWallet(), loadWithdraws()]);
  });
}

async function onPostImage(e: Event) {
  if (!requireLogin()) return;
  const input = e.target as HTMLInputElement;
  const files = Array.from(input.files || []).slice(0, 9 - postImages.value.length);
  await runBusy(async () => {
    for (const f of files) postImages.value.push(await upload(f));
  });
  input.value = "";
}

async function publishPost() {
  if (!requireLogin()) return;
  if (!postContent.value.trim() && postImages.value.length === 0) return showToast("写点内容或上传图片");
  await runBusy(async () => {
    await api("/api/mp/plaza/posts", {
      method: "POST",
      body: { content: postContent.value, gender: "AUTO", category: "AUTO", images: postImages.value },
    });
    postContent.value = "";
    postImages.value = [];
    composerOpen.value = false;
    showToast("发布成功");
    await loadPosts(true);
  });
}

async function toggleLike(post: PlazaPost) {
  if (!requireLogin()) return;
  await runBusy(async () => {
    if (post.liked) {
      await api(`/api/mp/plaza/posts/${post.id}/like`, { method: "DELETE" });
      post.liked = false;
      post.likeCount = Math.max(0, post.likeCount - 1);
    } else {
      await api(`/api/mp/plaza/posts/${post.id}/like`, { method: "POST" });
      post.liked = true;
      post.likeCount += 1;
    }
  });
}

async function openComments(post: PlazaPost) {
  activePost.value = post;
  commentsOpen.value = true;
  await runBusy(async () => {
    comments.value = await api<CommentItem[]>(`/api/mp/public/plaza/posts/${post.id}/comments`);
  });
}

async function openPostDetail(post: PlazaPost) {
  detailPost.value = post;
  postDetailOpen.value = true;
  await runBusy(async () => {
    detailComments.value = await api<CommentItem[]>(`/api/mp/public/plaza/posts/${post.id}/comments`);
  });
}

async function sharePost(post: PlazaPost) {
  const url = `${window.location.origin}${window.location.pathname}#post-${post.id}`;
  const title = `${post.authorName || "匿名用户"}的动态`;
  const text = post.content || "分享了一条动态";
  try {
    if (navigator.share) {
      await navigator.share({ title, text, url });
      return;
    }
    await navigator.clipboard.writeText(`${title}\n${text}\n${url}`);
    showToast("链接已复制");
  } catch {
    showToast("分享已取消");
  }
}

async function submitComment() {
  if (!requireLogin() || !activePost.value) return;
  if (!commentText.value.trim()) return;
  await runBusy(async () => {
    await api(`/api/mp/plaza/posts/${activePost.value?.id}/comments`, {
      method: "POST",
      body: { content: commentText.value },
    });
    commentText.value = "";
    if (activePost.value) {
      activePost.value.commentCount += 1;
      comments.value = await api<CommentItem[]>(`/api/mp/public/plaza/posts/${activePost.value.id}/comments`);
    }
  });
}

async function openTaskDetail(task: TaskItem | OrderItem) {
  const taskNo = "taskNo" in task ? task.taskNo : "";
  if (!taskNo) return;
  taskDetailOpen.value = true;
  await runBusy(async () => {
    taskDetail.value = await api<TaskDetail>(`/api/mp/tasks/${taskNo}`);
    taskSubmissions.value = await api<TaskSubmission[]>(`/api/mp/public/tasks/${taskNo}/submissions`);
    myOrder.value = null;
    if (token.value) {
      try {
        myOrder.value = await api(`/api/mp/tasks/${taskNo}/my-order`);
        taskSubmissions.value = await api<TaskSubmission[]>(`/api/mp/tasks/${taskNo}/submissions`);
      } catch {
        myOrder.value = null;
      }
    }
  });
}

async function toggleSubmissionLike(item: TaskSubmission) {
  if (!requireLogin() || !taskDetail.value) return;
  await runBusy(async () => {
    const res = await api<{ liked: boolean; likeCount: number }>(
      `/api/mp/tasks/${taskDetail.value?.taskNo}/submissions/${item.orderNo}/like`,
      { method: "POST", body: { like: !item.likedByMe } },
    );
    item.likedByMe = res.liked;
    item.likeCount = res.likeCount;
  });
}

async function publishTask() {
  if (!requireLogin()) return;
  if (!taskForm.value.title.trim() || !taskForm.value.content.trim()) return showToast("请填写任务标题和内容");
  const deadlineAt = taskForm.value.deadlineAt
    ? new Date(taskForm.value.deadlineAt).toISOString()
    : new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString();
  await runBusy(async () => {
    try {
      await api("/api/mp/tasks", { method: "POST", body: { ...taskForm.value, category: "AUTO", deadlineAt } });
    } catch (e) {
      const msg = e instanceof Error ? e.message : "";
      if (msg.includes("Insufficient balance")) {
        rechargeAmount.value = taskForm.value.amount || "9.90";
        rechargeAfterTask.value = true;
        await loadRechargeConfig();
        rechargeOpen.value = true;
        showToast("余额不足，请先充值");
        return;
      }
      throw e;
    }
    publishOpen.value = false;
    rechargeAfterTask.value = false;
    showToast("任务已提交审核");
    await Promise.all([loadTasks(true), loadMyOrders(true), loadWallet(), loadWalletFlows()]);
  });
}

function closeRecharge() {
  if (rechargePollTimer) {
    clearInterval(rechargePollTimer);
    rechargePollTimer = null;
  }
  rechargeOpen.value = false;
  rechargeQrDataUrl.value = "";
  rechargeOutTradeNo.value = "";
  rechargeQrStatus.value = "";
}

function startRechargePoll() {
  if (rechargePollTimer) clearInterval(rechargePollTimer);
  rechargePollTimer = setInterval(async () => {
    if (!rechargeOutTradeNo.value) return;
    try {
      const res = await api<{ status: string }>(`/api/mp/wallet/recharge/status/${rechargeOutTradeNo.value}`);
      if (res.status === "SUCCESS") {
        clearInterval(rechargePollTimer!);
        rechargePollTimer = null;
        rechargeQrStatus.value = "success";
        closeRecharge();
        showToast("充值成功");
        await Promise.all([loadWallet(), loadWalletFlows()]);
        if (rechargeAfterTask.value) {
          rechargeAfterTask.value = false;
          await publishTask();
        } else if (rechargeAfterMusic.value) {
          rechargeAfterMusic.value = false;
          await generateMusic();
        } else if (rechargeAfterPackage.value) {
          rechargeAfterPackage.value = false;
          await buyMusicPackage();
        }
      }
    } catch {
      // ignore poll errors
    }
  }, 2000);
}

async function completeRecharge() {
  if (!requireLogin()) return;
  if (!rechargeAmount.value || Number(rechargeAmount.value) <= 0) return showToast("请输入充值金额");
  if (rechargeConfig.value?.channel === "WXPAY_NATIVE") {
    await runBusy(async () => {
      const res = await api<{ outTradeNo: string; codeUrl: string }>(
        "/api/mp/wallet/recharge/native-prepay",
        { method: "POST", body: { amount: rechargeAmount.value } }
      );
      rechargeOutTradeNo.value = res.outTradeNo;
      rechargeQrDataUrl.value = await QRCode.toDataURL(res.codeUrl, { width: 200 });
      rechargeQrStatus.value = "pending";
      startRechargePoll();
    });
  } else {
    await runBusy(async () => {
      await api("/api/mp/wallet/recharge/mock-success", { method: "POST", body: { amount: rechargeAmount.value } });
      closeRecharge();
      showToast("充值成功");
      await Promise.all([loadWallet(), loadWalletFlows()]);
      if (rechargeAfterTask.value) {
        rechargeAfterTask.value = false;
        await publishTask();
      } else if (rechargeAfterMusic.value) {
        rechargeAfterMusic.value = false;
        await generateMusic();
      } else if (rechargeAfterPackage.value) {
        rechargeAfterPackage.value = false;
        await buyMusicPackage();
      }
    });
  }
}

async function acceptTask(task?: TaskItem | TaskDetail | null) {
  if (!requireLogin() || !task) return;
  await runBusy(async () => {
    const res = await api<{ orderNo: string; status: string }>(`/api/mp/tasks/${task.taskNo}/accept`, { method: "POST" });
    myOrder.value = { taskNo: task.taskNo, orderNo: res.orderNo, orderStatus: res.status };
    showToast("接单成功");
    await Promise.all([loadTasks(true), loadMyOrders(true)]);
  });
}

async function onProofImage(e: Event) {
  if (!requireLogin()) return;
  const input = e.target as HTMLInputElement;
  const files = Array.from(input.files || []).slice(0, 4 - proofImages.value.length);
  if (!files.length) return;
  await runBusy(async () => {
    for (const f of files) proofImages.value.push(await upload(f));
  });
  input.value = "";
}

async function submitOrderProof() {
  if (!requireLogin() || !myOrder.value) return;
  if (!proofText.value.trim() && !proofImages.value.length) return showToast("请填写文字凭证或上传图片");
  const proofs: { type: string; url: string; remark: string }[] = [];
  if (proofText.value.trim()) proofs.push({ type: "TEXT", url: "TEXT_CONTENT", remark: proofText.value.trim() });
  proofImages.value.forEach((url, index) => proofs.push({ type: "IMAGE", url, remark: `图片凭证${index + 1}` }));
  await runBusy(async () => {
    await api(`/api/mp/orders/${myOrder.value?.orderNo}/submit`, { method: "POST", body: { proofs } });
    submitProofOpen.value = false;
    proofText.value = "";
    proofImages.value = [];
    showToast("凭证已提交");
    if (taskDetail.value) await openTaskDetail(taskDetail.value);
    await loadMyOrders(true);
  });
}

async function init() {
  await loadMe();
  await Promise.all([loadHome(), loadPlazaMeta(), loadPosts(true), loadTasks(true), loadWallet(), loadFollows(), loadUnreadMessageCount()]);
}

onMounted(() => {
  void init();
  clockTimer = window.setInterval(() => {
    nowTs.value = Date.now();
  }, 1000);
  musicPollTimer = window.setInterval(() => {
    void pollPendingMusicJobs();
  }, 10000);
  musicProgressTimer = window.setInterval(() => {
    tickMusicGenerationProgress();
  }, 700);
  messagePollTimer = window.setInterval(() => {
    void loadUnreadMessageCount();
  }, 15000);
  window.addEventListener("scroll", onWindowScroll, { passive: true });
  window.addEventListener("touchstart", onTouchStart, { passive: true });
  window.addEventListener("touchend", onTouchEnd, { passive: true });
});

onBeforeUnmount(() => {
  if (clockTimer) window.clearInterval(clockTimer);
  if (musicPollTimer) window.clearInterval(musicPollTimer);
  if (musicProgressTimer) window.clearInterval(musicProgressTimer);
  if (messagePollTimer) window.clearInterval(messagePollTimer);
  window.removeEventListener("scroll", onWindowScroll);
  window.removeEventListener("touchstart", onTouchStart);
  window.removeEventListener("touchend", onTouchEnd);
});
</script>

<template>
  <div class="app-shell">
    <div class="gradient-line"></div>
    <header class="topbar">
      <div class="brand" @click="switchTab('plaza')">
        <span class="brand-mark"><img src="/favicon.svg" alt="" /></span>
        <span class="brand-text">叼瓜赖圈</span>
      </div>
      <div class="top-actions">
        <button class="icon-btn notify-btn" title="通知" @click="openMessages">
          <Bell :size="18" />
          <span v-if="unreadMessageCount > 0" class="red-dot"></span>
        </button>
        <button v-if="!isLoggedIn" class="primary small" @click="loginOpen = true">登录</button>
        <button v-else class="avatar-chip" @click="switchTab('me')">
          <span>
            <img v-if="me?.avatar" :src="fileUrl(me.avatar)" alt="" />
            <template v-else>{{ (me?.nickname || "我").slice(0, 1) }}</template>
          </span>
          <b>{{ me?.nickname }}</b>
        </button>
      </div>
    </header>

    <main class="layout">
      <aside class="left-rail">
        <button :class="{ active: activeTab === 'plaza' }" @click="switchTab('plaza')"><Home :size="18" /><span>广场</span><span v-if="navHot('plaza')" class="nav-hot">🔥</span></button>
        <button :class="{ active: activeTab === 'tasks' }" @click="switchTab('tasks')"><Target :size="18" /><span>任务</span><span v-if="navHot('tasks')" class="nav-hot">🔥</span></button>
        <button :class="{ active: activeTab === 'music' }" @click="switchTab('music')"><Music2 :size="18" /><span>音乐</span><span v-if="navHot('music')" class="nav-hot">🔥</span></button>
        <button :class="{ active: activeTab === 'me' }" @click="switchTab('me')"><UserRound :size="18" /><span>我的</span><span v-if="navHot('me')" class="nav-hot">🔥</span></button>
        <button class="hot-action" @click="openComposer"><Plus :size="18" />发动态 <span>NEW</span></button>
      </aside>

      <section class="feed">
        <div v-if="activeTab === 'plaza'" class="view-stack">
          <div class="hero-card">
            <div>
              <span class="badge hot"><Flame :size="13" />热</span>
              <h1>AI 社区动态广场</h1>
              <p>{{ homeData.notices[0]?.title || "刷动态、看 AI 自动评论，也可以分享本地生活与任务线索。" }}</p>
            </div>
            <button class="primary" @click="openComposer"><Plus :size="18" />发布动态</button>
          </div>

          <div v-if="homeData.banners.length || plazaMeta.aiProviders.length" class="chip-card">
            <b v-if="plazaMeta.aiProviders.length" class="chip-title ai-title">常驻AI</b>
            <img v-for="b in homeData.banners" :key="b.id" :src="fileUrl(b.imageUrl)" alt="" />
            <span v-for="ai in plazaMeta.aiProviders" :key="ai.code" class="ai-chip">
              <img v-if="ai.logoUrl" :src="fileUrl(ai.logoUrl)" alt="" />
              <i v-else>{{ aiLogoText(ai) }}</i>
              {{ ai.name }}
            </span>
          </div>

          <div class="pill-row sort-row">
            <button v-for="s in plazaMeta.sorts" :key="s.code" :class="{ active: postSort === s.code }" @click="postSort = s.code; loadPosts(true)">{{ s.name }}</button>
          </div>
          <div class="pill-row category-row">
            <button v-for="c in filterOptions" :key="c.code" :class="{ active: postFilter === c.code }" @click="postFilter = c.code; loadPosts(true)">{{ c.name }}</button>
          </div>

          <article v-for="post in posts" :key="post.id" class="post-card">
            <div class="post-head">
              <button class="avatar plain" @click="openUserCard(post.authorId)">
                <img v-if="post.authorAvatar" :src="fileUrl(post.authorAvatar)" alt="" />
                <span v-else>{{ avatarText(post.authorName) }}</span>
              </button>
              <div>
                <div class="name">{{ post.authorName || "匿名用户" }}</div>
                <div class="meta">{{ plazaMetaText(post) }}</div>
              </div>
              <button v-if="me?.id !== post.authorId" class="follow-mini" :class="{ active: post.followed }" @click="toggleFollowPost(post)">
                {{ post.followed ? "已关注" : "关注" }}
              </button>
            </div>
            <p class="post-text">{{ post.content || "分享了图片动态" }}</p>
            <div v-if="post.images?.length" class="image-grid" :class="'cols-' + Math.min(3, post.images.length)">
              <img v-for="img in post.images" :key="img" :src="fileUrl(img)" alt="" />
            </div>
            <div v-if="post.music" class="music-embed" :class="{ deleted: musicAttachmentDeleted(post.music) }">
              <div class="music-embed-head">
                <img v-if="post.music.imageUrl" :src="fileUrl(post.music.imageUrl)" alt="" />
                <Music2 v-else :size="22" />
                <div>
                  <b>{{ post.music.title }}</b>
                  <span v-if="musicAttachmentDeleted(post.music)">作者已删除音乐</span>
                  <span v-else>作者 {{ post.music.authorName || post.authorName }} · 作曲 {{ post.music.composer || "AI音乐生成" }} · 作词 {{ post.music.lyricist || "AI作词" }}</span>
                </div>
              </div>
              <button v-if="musicAttachmentReady(post.music)" class="player-inline-play" @click.stop="playSong({ ...post.music, authorName: post.music.authorName || post.authorName, createdAt: post.createdAt })">
                <Pause v-if="isSongPlaying(post.music)" :size="16" /><Play v-else :size="16" />{{ playButtonLabel({ ...post.music, authorName: post.music.authorName || post.authorName }) }}
              </button>
            </div>
            <div class="action-row">
              <button :class="{ liked: post.liked }" @click="toggleLike(post)"><Heart :size="18" />{{ post.likeCount }}</button>
              <button @click="openComments(post)"><MessageCircle :size="18" />{{ post.commentCount }}</button>
              <button @click="sharePost(post)"><Share2 :size="18" />分享</button>
              <span v-if="isHotPost(post)" class="hot-mark"><Flame :size="16" />热门</span>
            </div>
          </article>
          <button v-if="postHasMore" class="load-more" @click="loadPosts()">{{ paging && activeTab === "plaza" ? "正在加载..." : "继续下滑自动加载" }}</button>
          <div v-else class="empty">没有更多动态了</div>
        </div>

        <div v-if="activeTab === 'tasks'" class="view-stack">
          <div class="hero-card task-hero">
            <div>
              <span class="badge new"><Zap :size="13" />爆单</span>
              <h1>任务大厅</h1>
              <p>找轻任务、接本地单，提交凭证后等待审核结算。</p>
            </div>
            <button class="accent" @click="openPublishTask"><Plus :size="18" />发布任务</button>
          </div>
          <div class="search-card">
            <Search :size="18" />
            <input v-model="taskQuery" placeholder="搜索任务标题、编号、地点" @keyup.enter="loadTasks(true)" />
            <button @click="loadTasks(true)">搜索</button>
          </div>
          <div class="pill-row">
            <button :class="{ active: taskSort === 'LATEST' }" @click="taskSort = 'LATEST'; loadTasks(true)">最新</button>
            <button :class="{ active: taskSort === 'AMOUNT' }" @click="taskSort = 'AMOUNT'; loadTasks(true)">金额最高</button>
            <button :class="{ active: taskSort === 'PEOPLE' }" @click="taskSort = 'PEOPLE'; loadTasks(true)">人数最多</button>
            <button :class="{ active: taskSort === 'ENDED' }" @click="taskSort = 'ENDED'; loadTasks(true)">已结束</button>
          </div>
          <article v-for="task in tasks" :key="task.taskNo" class="task-card" @click="openTaskDetail(task)">
            <div class="task-main">
              <span class="badge soft">{{ task.category || "轻任务" }}</span>
              <h2>{{ task.title }}</h2>
              <p>{{ task.taskNo }} · {{ task.locationText || "线上" }} · 发布 {{ fmtTime(task.createdAt) }} · 截止 {{ fmtTime(task.deadlineAt) }}</p>
              <div class="countdown" :class="{ ended: countdownText(task.deadlineAt) === '已结束' }">距离结束 {{ countdownText(task.deadlineAt) }}</div>
              <div class="progress"><span :style="{ width: Math.min(100, (task.acceptedSlots / Math.max(1, task.totalSlots)) * 100) + '%' }"></span></div>
            </div>
            <div class="task-side">
              <div class="task-reward-line">
                <div class="price">¥{{ task.amount }}</div>
                <div class="meta">{{ task.acceptedSlots }}/{{ task.totalSlots }} 人</div>
              </div>
              <button class="primary small" :disabled="task.status === 'EXPIRED' || countdownText(task.deadlineAt) === '已结束'" @click.stop="task.myOrderNo ? openTaskDetail(task) : acceptTask(task)">
                {{ task.status === 'EXPIRED' || countdownText(task.deadlineAt) === '已结束' ? "已结束" : (task.myOrderNo ? statusText(task.myOrderStatus) : "立即接单") }}
              </button>
            </div>
          </article>
          <button v-if="taskHasMore" class="load-more" @click="loadTasks()">{{ paging && activeTab === "tasks" ? "正在加载..." : "继续下滑自动加载" }}</button>
          <div v-else class="empty">没有更多任务了</div>
        </div>

        <div v-if="activeTab === 'music'" class="view-stack">
          <div class="music-lab">
            <div class="music-lab-head">
              <div>
                <span class="badge new"><Sparkles :size="13" />AI音乐实验室</span>
                <h1>一句话，生成一首完整歌曲</h1>
                <p v-if="musicCredits?.freeWeek">上线前一周免费生成，支持歌词、风格、人声/纯音乐和语言设置。</p>
                <p v-else>今日免费 {{ musicCredits?.dailyFreeRemaining ?? 0 }}/{{ musicCredits?.dailyFreeTotal ?? 2 }} 次 · 套餐剩余 {{ musicCredits?.packageRemaining ?? 0 }} 次 · 用完 ¥{{ musicCredits?.paidPrice || "1.20" }}/次。</p>
              </div>
              <button class="primary" @click="loadMusicJobs"><RefreshCw :size="18" />刷新</button>
            </div>

            <div class="music-composer-grid">
              <div class="music-form">
                <div class="music-form-toolbar">
                  <div class="music-mode-tabs">
                    <button :class="{ active: !musicForm.custom_mode }" @click="musicForm.custom_mode = false">灵感生成</button>
                    <button :class="{ active: musicForm.custom_mode }" @click="musicForm.custom_mode = true">专业填词</button>
                  </div>
                  <button class="music-toggle" :class="{ active: musicForm.instrumental }" @click="musicForm.instrumental = !musicForm.instrumental">
                    <Music2 :size="15" />{{ musicForm.instrumental ? "纯音乐已开" : "纯音乐" }}
                  </button>
                </div>
                <div class="form">
                  <input v-model="musicForm.title" maxlength="100" placeholder="歌曲标题，例如：今晚不想睡" />
                  <textarea v-model="musicForm.prompt" rows="5" maxlength="5000" style="height: 200px" :placeholder="musicForm.custom_mode ? '粘贴完整歌词，或点击 AI 作词生成一版歌词。' : '描述你想要的歌曲：心情、场景、男女声、风格，例如：失恋后深夜听的伤感情歌，女声，慢节奏。'" />
                  <select v-model="musicForm.lang" class="music-lang-select">
                    <option v-for="lang in musicLanguages" :key="lang.value" :value="lang.value">{{ lang.label }}</option>
                  </select>
                  <input v-model="musicForm.style" maxlength="1000" placeholder="点下方风格标签选择，也可以输入自定义风格，例如 lo-fi bedroom pop / city pop / 梦核" />
                  <div class="music-actions">
                    <button class="ghost small" @click="assistMusic"><Sparkles :size="15" />AI作词</button>
                    <button class="accent" :disabled="musicSubmitting" @click="generateMusic"><Music2 :size="18" />{{ musicSubmitting ? "提交中" : "生成音乐" }}</button>
                  </div>
                </div>
              </div>

              <aside class="music-prompt-panel">
                <div class="panel-title">灵感包</div>
                <div class="music-inspire-grid">
                  <button v-for="p in visibleMusicPacks" :key="p.title" class="music-inspire" @click="useMusicInspiration(p)">
                    <span>{{ p.title }}</span>
                  </button>
                </div>
                <button class="music-more-btn" @click="showAllMusicPacks = !showAllMusicPacks">{{ showAllMusicPacks ? "收起灵感" : "展开更多灵感" }}</button>
                <div class="panel-title">风格标签</div>
                <div class="music-style-tabs">
                  <button v-for="group in musicStyleGroups" :key="group.code" :class="{ active: musicStyleGroup === group.code }" @click="musicStyleGroup = group.code">{{ group.label }}</button>
                </div>
                <div class="music-style-cloud">
                  <button v-for="s in activeMusicStyleItems" :key="s[0]" :class="{ active: musicStyleSelected(s[0]) }" @click="toggleMusicStyle(s[0])">{{ s[1] }}</button>
                </div>
              </aside>
            </div>

            <div v-if="musicPackages.length" class="music-package-strip">
              <button v-for="p in musicPackages" :key="p.code" class="package-pill" @click="openPackagePurchase(p)">
                <span>{{ p.name }}</span>
                <small>{{ p.credits }}次 · ¥{{ p.price }} · {{ p.discountText }}</small>
              </button>
            </div>
          </div>

          <section class="music-section">
            <div class="music-section-head">
              <div>
                <span class="badge soft">我的作品</span>
                <h2>生成记录</h2>
              </div>
              <button
                v-if="foldedMusicJobs.length"
                class="ghost small music-history-toggle"
                @click="musicHistoryExpanded = !musicHistoryExpanded"
              >
                <ChevronDown v-if="musicHistoryExpanded" :size="16" />
                <ChevronRight v-else :size="16" />
                {{ musicHistoryExpanded ? "收起历史记录" : `展开历史记录 ${foldedMusicJobs.length} 条` }}
              </button>
            </div>

            <div class="music-my-list">
              <article v-for="job in visibleMusicJobs" :key="job.id" class="music-my-item">
                <div class="music-my-card">
                  <div class="music-my-cover">
                    <img v-if="job.imageUrl" :src="fileUrl(job.imageUrl)" alt="" />
                    <Music2 v-else :size="34" />
                    <button v-if="musicPlayable(job)" class="music-cover-play" @click="playSong(job)">
                      <Pause v-if="currentSong?.id === job.id && playerPlaying" :size="22" />
                      <Play v-else :size="22" />
                    </button>
                  </div>

                  <div class="music-my-body">
                    <div class="music-my-head">
                      <div class="music-title-line">
                        <div class="name">{{ job.title }}</div>
                        <div class="music-my-author">
                          <img v-if="job.authorAvatar" :src="fileUrl(job.authorAvatar)" alt="" />
                          <span v-else>{{ avatarText(job.authorName || me?.nickname) }}</span>
                          <b>{{ job.authorName || me?.nickname || "匿名用户" }}</b>
                          <small>·</small>
                          <small>{{ musicTotalDuration(job) ? fmtDuration(musicTotalDuration(job)) : "--:--" }}</small>
                          <small>·</small>
                          <small>{{ musicStyleTags(job, 3).join(", ") || job.lang || "AI Music" }}</small>
                          <button class="music-like-float music-like-mobile" :class="{ liked: job.liked }" @click="rateMusic(job, 5)"><Heart :size="18" /></button>
                        </div>
                      </div>
                      <button class="music-like-float music-like-desktop" :class="{ liked: job.liked }" @click="rateMusic(job, 5)"><Heart :size="18" /></button>
                    </div>

                    <div class="music-my-progress">
                      <input
                        v-if="musicPlayable(job)"
                        :class="{ playing: currentSong?.id === job.id && playerPlaying }"
                        type="range"
                        min="0"
                        max="100"
                        step="0.1"
                        :value="musicCardProgress(job)"
                        :style="{ '--progress': musicCardProgress(job) + '%' }"
                        @input="seekMusicCard(job, $event)"
                      />
                      <div v-else><span :style="{ width: musicCardProgress(job) + '%' }"></span></div>
                      <p><em>{{ musicCardElapsed(job) }}</em><em>{{ musicCardRemaining(job) }}</em></p>
                    </div>

                    <div v-if="!musicPlayable(job)" class="music-generate-progress">
                      <p>
                        <span>{{ musicGenerationLabel(job) }}</span>
                        <b>{{ musicGenerationPercent(job) }}%</b>
                      </p>
                      <div><span :style="{ width: musicGenerationPercent(job) + '%' }"></span></div>
                    </div>
                    <div class="music-my-actions">
                      <button class="ghost small" @click="useSongAsTemplate(job)"><Sparkles :size="15" />做同款</button>
                      <button class="ghost small" @click="copyMusicStyle(job)">复制风格</button>
                      <button class="ghost small" @click="todoMusicFeature">🎛 分轨</button>
                    </div>
                  </div>
                </div>

                <div class="music-my-manage">
                  <button class="ghost small" @click="renameMusic(job)">改名</button>
                  <button class="ghost small" @click="deleteMusic(job)">删除</button>
                  <a v-if="job.audioUrl" class="ghost small" :href="fileUrl(job.audioUrl)" target="_blank" rel="noreferrer">MP3下载</a>
                  <button class="ghost small" @click="downloadLyrics(job)">歌词下载</button>
                  <button class="ghost small" @click="publishMusic(job)">{{ job.published ? "已发布广场" : "发布到音乐广场" }}</button>
                  <button class="ghost small" @click="shareMusic(job)">{{ job.plazaPostId ? "已同步到广场" : "同步到广场" }}</button>
                  <button class="ghost small" @click="downloadCover(job)">封面下载</button>
                  <button class="primary sell" @click="todoMusicFeature">去售卖</button>
                  <button class="ghost small" @click="shareUserMusic(job)">分享</button>
                  <button class="accent wav" @click="todoMusicFeature">生成高清无损WAV</button>
                  <button class="ghost small license" @click="todoMusicFeature">商业授权</button>
                  <button class="ghost small" @click="todoMusicFeature">🎛 分轨</button>
                </div>

                <details v-if="job.lyrics" class="music-my-lyrics-detail">
                  <summary>歌词</summary>
                  <p>{{ job.lyrics }}</p>
                </details>

                <div v-if="job.errorMessage" class="error-text">{{ job.errorMessage }}</div>
              </article>
            </div>

            <div v-if="musicLoaded && !musicJobs.length" class="empty">暂无音乐生成记录</div>
          </section>

          <div class="music-section-head">
            <div>
              <span class="badge soft">音乐广场</span>
              <h2>大家正在听</h2>
            </div>
            <div class="pill-row compact">
              <button :class="{ active: musicHallSort === 'hot' }" @click="musicHallSort = 'hot'; loadMusicJobs()">热门</button>
              <button :class="{ active: musicHallSort === 'new' }" @click="musicHallSort = 'new'; loadMusicJobs()">最新</button>
            </div>
          </div>
          <article v-for="song in musicHall" :key="'hall-' + song.id" class="music-my-item">
            <div class="music-my-card music-hall-card">
              <div class="music-my-cover">
              <img v-if="song.imageUrl" :src="fileUrl(song.imageUrl)" alt="" />
                <Music2 v-else :size="34" />
                <button v-if="musicPlayable(song)" class="music-cover-play" @click="playSong(song)">
                  <Pause v-if="currentSong?.id === song.id && playerPlaying" :size="22" />
                  <Play v-else :size="22" />
                </button>
              </div>
              <div class="music-my-body">
                <div class="music-my-head">
                <div class="music-title-line">
                  <div class="name">{{ song.title }}</div>
                    <div class="music-my-author">
                      <img v-if="song.authorAvatar" :src="fileUrl(song.authorAvatar)" alt="" />
                      <span v-else>{{ avatarText(song.authorName) }}</span>
                      <b>{{ song.authorName || "匿名用户" }}</b>
                      <small>·</small>
                      <small>{{ musicTotalDuration(song) ? fmtDuration(musicTotalDuration(song)) : "--:--" }}</small>
                      <small>·</small>
                      <small>{{ musicStyleTags(song, 3).join(", ") || song.lang || "AI Music" }}</small>
                      <button class="music-like-float music-like-mobile" :class="{ liked: song.liked }" @click="rateMusic(song, 5)"><Heart :size="18" /></button>
                    </div>
                  </div>
                  <button class="music-like-float music-like-desktop" :class="{ liked: song.liked }" @click="rateMusic(song, 5)"><Heart :size="18" /></button>
                </div>

                <div class="music-my-progress">
                  <input
                    v-if="musicPlayable(song)"
                    :class="{ playing: currentSong?.id === song.id && playerPlaying }"
                    type="range"
                    min="0"
                    max="100"
                    step="0.1"
                    :value="musicCardProgress(song)"
                    :style="{ '--progress': musicCardProgress(song) + '%' }"
                    @input="seekMusicCard(song, $event)"
                  />
                  <div v-else><span :style="{ width: musicCardProgress(song) + '%' }"></span></div>
                  <p><em>{{ musicCardElapsed(song) }}</em><em>{{ musicCardRemaining(song) }}</em></p>
                </div>

                <div class="music-my-actions">
                  <button class="ghost small" @click="useSongAsTemplate(song)"><Sparkles :size="15" />做同款</button>
                  <button class="ghost small" @click="copyMusicStyle(song)">复制风格</button>
                </div>
              </div>
            </div>
              <div class="action-row">
                <button :class="{ liked: song.liked }" @click="rateMusic(song, 5)"><Heart :size="18" />{{ song.ratingCount || 0 }}</button>
                <button @click="toggleUserMusicComments(song)"><MessageCircle :size="18" />{{ userMusicCommentsOpen[song.id] ? "收起" : "评论" }}</button>
                <button @click="shareUserMusic(song)"><Share2 :size="18" />分享</button>
              </div>
              <div v-if="userMusicCommentsOpen[song.id]" class="comments inline-comments music-comments">
                <div v-for="c in userMusicComments[song.id] || []" :key="c.id" class="comment">
                  <div class="comment-avatar" :class="{ 'ai-comment-avatar': isAiComment(c.userName) }">
                    <img v-if="c.userAvatar" :src="fileUrl(c.userAvatar)" alt="" />
                    <span v-else>{{ c.userName?.slice(0, 1) || "匿" }}</span>
                  </div>
                  <div><b>{{ c.userName }}</b><p>{{ c.content }}</p><span>{{ fmtTime(c.createdAt) }}</span></div>
                </div>
                <div v-if="!(userMusicComments[song.id] || []).length" class="empty">暂无评论</div>
                <div class="comment-box"><input v-model="userMusicCommentText[song.id]" placeholder="写评论..." /><button @click="submitUserMusicComment(song)"><Send :size="17" /></button></div>
              </div>
          </article>
        </div>

        <div v-if="activeTab === 'me'" class="view-stack">
          <div class="profile-card">
            <div class="profile-avatar">
              <img v-if="me?.avatar" :src="fileUrl(me.avatar)" alt="" />
              <span v-else>{{ (me?.nickname || "未").slice(0, 1) }}</span>
            </div>
            <div>
              <h1>{{ me?.nickname || "未登录用户" }}</h1>
              <p v-if="isLoggedIn">ID {{ me?.id }} · {{ genderText(me?.gender) }} · {{ me?.status }} · 信用分 {{ me?.creditScore }}</p>
              <p v-else>登录后查看钱包、任务和动态</p>
              <p v-if="me?.signature">{{ me.signature }}</p>
            </div>
            <div class="profile-actions">
              <button v-if="!isLoggedIn" class="primary" @click="loginOpen = true">立即登录</button>
              <template v-else>
                <button class="ghost" @click="editProfileOpen = true"><Edit3 :size="16" />编辑</button>
                <button class="ghost" @click="logout"><LogOut :size="16" />退出</button>
              </template>
            </div>
          </div>
          <div class="stats-grid">
            <div class="stat-card"><WalletCards :size="20" /><b>{{ isLoggedIn ? `¥${wallet?.balance || "0.00"}` : "--" }}</b><span>可用余额</span></div>
            <div class="stat-card"><QrCode :size="20" /><b>{{ isLoggedIn ? `¥${wallet?.frozenAmount || "0.00"}` : "--" }}</b><span>冻结金额</span></div>
            <div class="stat-card"><CheckCircle2 :size="20" /><b>{{ isLoggedIn ? `¥${wallet?.totalIncome || "0.00"}` : "--" }}</b><span>累计收益</span></div>
            <div class="stat-card"><Flame :size="20" /><b>{{ isLoggedIn ? (followSummary?.followerCount || 0) : "--" }}</b><span>粉丝</span></div>
          </div>
          <div class="pill-row">
            <button :class="{ active: mePanel === 'overview' }" @click="switchMePanel('overview')">概览</button>
            <button :class="{ active: mePanel === 'posts' }" @click="switchMePanel('posts')">我的动态</button>
            <button :class="{ active: mePanel === 'orders' }" @click="switchMePanel('orders')">我的接单</button>
            <button class="tab-with-dot" :class="{ active: mePanel === 'messages' }" @click="switchMePanel('messages')">消息<span v-if="unreadMessageCount > 0" class="red-dot inline"></span></button>
            <button :class="{ active: mePanel === 'wallet' }" @click="switchMePanel('wallet')">钱包</button>
            <button :class="{ active: mePanel === 'follows' }" @click="switchMePanel('follows')">关注</button>
          </div>

          <div v-if="mePanel === 'overview'" class="quick-grid">
            <button @click="switchMePanel('posts')">我的动态 <ChevronRight :size="16" /></button>
            <button @click="switchMePanel('orders')">我的接单 <ChevronRight :size="16" /></button>
            <button class="quick-with-dot" @click="switchMePanel('messages')">消息通知 <span v-if="unreadMessageCount > 0" class="red-dot inline"></span><ChevronRight :size="16" /></button>
            <button @click="switchMePanel('wallet')">钱包明细 <ChevronRight :size="16" /></button>
            <button @click="editProfileOpen = true">编辑资料 <ChevronRight :size="16" /></button>
            <button @click="openFeedback">反馈建议 <ChevronRight :size="16" /></button>
          </div>

          <template v-if="mePanel === 'posts'">
            <article v-for="post in myPosts" :key="post.id" class="post-card compact clickable" @click="openPostDetail(post)">
              <div class="post-head">
                <div class="avatar small">{{ avatarText(post.authorName || me?.nickname) }}</div>
                <div>
                  <div class="name">{{ post.authorName || me?.nickname || "我" }}</div>
                  <div class="meta">{{ plazaMetaText(post) }}</div>
                </div>
                <span v-if="isHotPost(post)" class="hot-mark"><Flame :size="15" />热门</span>
              </div>
              <p class="post-text">{{ post.content || "图片动态" }}</p>
              <div v-if="post.images?.length" class="image-grid" :class="'cols-' + Math.min(3, post.images.length)">
                <img v-for="img in post.images" :key="img" :src="fileUrl(img)" alt="" />
              </div>
              <div v-if="post.music" class="music-embed" :class="{ deleted: musicAttachmentDeleted(post.music) }">
                <div class="music-embed-head">
                  <img v-if="post.music.imageUrl" :src="fileUrl(post.music.imageUrl)" alt="" />
                  <Music2 v-else :size="22" />
                  <div>
                    <b>{{ post.music.title }}</b>
                    <span v-if="musicAttachmentDeleted(post.music)">作者已删除音乐</span>
                    <span v-else>作者 {{ post.music.authorName || post.authorName }} · 作曲 {{ post.music.composer || "AI音乐生成" }} · 作词 {{ post.music.lyricist || "AI作词" }}</span>
                  </div>
                </div>
                <button v-if="musicAttachmentReady(post.music)" class="player-inline-play" @click.stop="playSong({ ...post.music, authorName: post.music.authorName || post.authorName, createdAt: post.createdAt })">
                  <Pause v-if="isSongPlaying(post.music)" :size="16" /><Play v-else :size="16" />{{ playButtonLabel({ ...post.music, authorName: post.music.authorName || post.authorName }) }}
                </button>
              </div>
              <div class="action-row">
                <button :class="{ liked: post.liked }" @click.stop="toggleLike(post)"><Heart :size="18" />{{ post.likeCount }}</button>
                <button @click.stop="openComments(post)"><MessageCircle :size="18" />{{ post.commentCount }}</button>
                <button @click.stop="sharePost(post)"><Share2 :size="18" />分享</button>
              </div>
            </article>
            <button v-if="myPostHasMore" class="load-more" @click="loadMyPosts()">{{ paging ? "正在加载..." : "继续下滑自动加载" }}</button>
          </template>

          <template v-if="mePanel === 'orders'">
            <article v-for="order in myOrders" :key="order.orderNo" class="list-card" @click="openTaskDetail(order)">
              <div><b>{{ order.taskTitle }}</b><p>{{ order.taskNo }} · {{ order.orderNo }} · ¥{{ order.amount }} · {{ fmtTime(order.acceptTime) }}</p><p v-if="order.auditReason">{{ order.auditReason }}</p></div>
              <span>{{ statusText(order.orderStatus) }}</span>
            </article>
            <button v-if="orderHasMore" class="load-more" @click="loadMyOrders()">{{ paging ? "正在加载..." : "继续下滑自动加载" }}</button>
          </template>

          <template v-if="mePanel === 'messages'">
            <div v-if="messages.length" class="message-toolbar">
              <span>{{ unreadMessageCount > 0 ? `未读 ${unreadMessageCount} 条` : "全部已读" }}</span>
              <button class="ghost small" :disabled="unreadMessageCount <= 0" @click="markAllMessagesRead">全部已读</button>
            </div>
            <article v-for="msg in messages" :key="msg.id" class="list-card" @click="markMessageRead(msg)">
              <div><b>{{ msg.title }}</b><p>{{ msg.content }}</p></div>
              <span :class="{ unread: !msg.read }">{{ msg.read ? "已读" : "未读" }}</span>
            </article>
            <div v-if="!messages.length" class="empty">暂无消息</div>
          </template>

          <template v-if="mePanel === 'wallet'">
            <div class="wallet-actions">
              <button class="primary" @click="withdrawOpen = true"><WalletCards :size="17" />申请提现</button>
              <button class="ghost" @click="loadRechargeConfig(); rechargeOpen = true"><QrCode :size="17" />充值</button>
              <span>{{ wallet?.withdrawQrCodeUrl ? "已保存收款码" : "未上传收款码" }}</span>
            </div>
            <div class="info-box">
              <b>冻结明细</b>
              <article v-for="f in frozenFlows" :key="f.type + f.bizNo + f.createdAt" class="list-card">
                <div><b>{{ f.label }} · ¥{{ f.amount }}</b><p>{{ f.bizNo || "-" }} · {{ fmtTime(f.createdAt) }}</p></div>
                <span>{{ statusText(f.status) }}</span>
              </article>
              <p v-if="!frozenFlows.length">暂无冻结记录</p>
            </div>
            <div class="info-box">
              <b>收支明细</b>
              <article v-for="f in ledgerFlows" :key="f.type + f.bizNo + f.createdAt" class="list-card">
                <div><b>{{ f.label }} · <span :class="flowTone(f)">{{ flowAmountText(f) }}</span></b><p>{{ f.bizNo || "-" }} · {{ fmtTime(f.createdAt) }}</p></div>
                <span>{{ statusText(f.status) }}</span>
              </article>
              <p v-if="!ledgerFlows.length">暂无收支记录</p>
            </div>
            <article v-for="w in withdraws" :key="w.applyNo" class="list-card" @click="openWithdrawDetail(w)">
              <div><b>¥{{ w.amount }} · {{ w.channel }}</b><p>{{ w.applyNo }} · {{ fmtTime(w.createdAt) }}</p><p v-if="w.auditReason">{{ w.auditReason }}</p></div>
              <span>{{ statusText(w.status) }}</span>
            </article>
            <div v-if="!withdraws.length" class="empty">暂无提现记录</div>
          </template>

          <template v-if="mePanel === 'follows'">
            <div class="follow-columns">
              <div>
                <h3>关注 {{ followSummary?.followingCount || 0 }}</h3>
                <button v-for="u in following" :key="u.userId" class="user-line" @click="openUserCard(u.userId)"><div class="avatar small"><img v-if="u.avatar" :src="fileUrl(u.avatar)" alt="" /><span v-else>{{ u.nickname?.slice(0, 1) || "匿" }}</span></div><span>{{ u.nickname }}</span></button>
              </div>
              <div>
                <h3>粉丝 {{ followSummary?.followerCount || 0 }}</h3>
                <button v-for="u in followers" :key="u.userId" class="user-line" @click="openUserCard(u.userId)"><div class="avatar small"><img v-if="u.avatar" :src="fileUrl(u.avatar)" alt="" /><span v-else>{{ u.nickname?.slice(0, 1) || "匿" }}</span></div><span>{{ u.nickname }}</span></button>
              </div>
            </div>
          </template>
        </div>
      </section>

      <aside class="right-panel">
        <div class="side-card">
          <span class="badge new">NEW</span>
          <h3>{{ homeData.notices[0]?.title || "快捷入口" }}</h3>
          <p>{{ homeData.notices[0]?.content || "动态、任务、我的页面都已接入后端真实接口和分页。" }}</p>
        </div>
        <div v-if="homeData.notices.length > 1" class="side-card">
          <h3>公告</h3>
          <p v-for="n in homeData.notices.slice(1, 4)" :key="n.id">{{ n.title }}：{{ n.content }}</p>
        </div>
        <div class="side-card">
          <h3>快捷操作</h3>
          <button @click="openComposer"><ImagePlus :size="16" />发一条动态</button>
          <button @click="openPublishTask"><Target :size="16" />发布任务</button>
          <button @click="activeTab = 'me'; switchMePanel('orders')"><CheckCircle2 :size="16" />我的接单</button>
          <button @click="openFeedback"><Settings :size="16" />反馈建议</button>
        </div>
      </aside>
    </main>

    <nav class="mobile-tabs">
      <button :class="{ active: activeTab === 'plaza' }" @click="switchTab('plaza')"><Home :size="20" /><span>广场</span><span v-if="navHot('plaza')" class="nav-hot">🔥</span></button>
      <button :class="{ active: activeTab === 'tasks' }" @click="switchTab('tasks')"><Target :size="20" /><span>任务</span><span v-if="navHot('tasks')" class="nav-hot">🔥</span></button>
      <button :class="{ active: activeTab === 'music' }" @click="switchTab('music')"><Music2 :size="20" /><span>音乐</span><span v-if="navHot('music')" class="nav-hot">🔥</span></button>
      <button :class="{ active: activeTab === 'me' }" @click="switchTab('me')"><UserRound :size="20" /><span>我的</span><span v-if="navHot('me')" class="nav-hot">🔥</span></button>
    </nav>

    <div v-if="loginOpen" class="overlay" @click.self="loginOpen = false">
      <div class="sheet">
        <button class="close" @click="loginOpen = false"><X :size="18" /></button>
        <h2>登录叼瓜赖圈</h2>
        <p>邮箱注册/登录后会自动生成昵称，手机号可后续在资料里保留。</p>
        <div v-if="registerStep === 'none'" class="login-switch">
          <button :class="{ active: loginMode === 'password' }" @click="loginMode = 'password'">密码登录</button>
          <button :class="{ active: loginMode === 'code' }" @click="loginMode = 'code'">验证码登录</button>
        </div>
        <div class="form">
          <input v-model="email" type="email" maxlength="128" placeholder="邮箱" />

          <template v-if="registerStep === 'password'">
            <input v-if="loginMode === 'code'" v-model="code" maxlength="6" placeholder="邮箱验证码" />
            <input v-model="password" type="password" maxlength="32" placeholder="设置密码（6-32位）" />
            <input v-model="passwordConfirm" type="password" maxlength="32" placeholder="再次输入密码" @keydown.enter="emailRegister" />
            <button class="primary full" @click="emailRegister">完成注册并登录</button>
            <button class="ghost full" @click="registerStep = 'none'; passwordConfirm = ''">返回登录</button>
          </template>

          <template v-else-if="loginMode === 'password'">
            <input v-model="password" type="password" maxlength="32" placeholder="密码" @keydown.enter="emailPasswordLogin" />
            <button class="primary full" @click="emailPasswordLogin">登录</button>
          </template>

          <template v-else>
            <div class="code-line">
              <input v-model="code" maxlength="6" placeholder="邮箱验证码" />
              <button :disabled="emailCodeLeft > 0" @click="sendEmailCode">{{ emailCodeLeft > 0 ? `${emailCodeLeft}s` : "获取" }}</button>
            </div>
            <button class="primary full" @click="emailCodeLogin">登录</button>
          </template>
        </div>
      </div>
    </div>

    <div v-if="withdrawDetailOpen" class="overlay" @click.self="withdrawDetailOpen = false">
      <div class="sheet">
        <button class="close" @click="withdrawDetailOpen = false"><X :size="18" /></button>
        <h2>提现详情</h2>
        <div v-if="withdrawDetail" class="detail-meta">
          <div><span>申请号</span><b>{{ withdrawDetail.applyNo }}</b></div>
          <div><span>金额</span><b>¥{{ withdrawDetail.amount }}</b></div>
          <div><span>状态</span><b>{{ statusText(withdrawDetail.status) }}</b></div>
          <div><span>申请时间</span><b>{{ fmtTime(withdrawDetail.createdAt) }}</b></div>
          <div><span>打款时间</span><b>{{ withdrawDetail.paidAt ? fmtTime(withdrawDetail.paidAt) : "-" }}</b></div>
          <div><span>备注</span><b>{{ withdrawDetail.payRemark || withdrawDetail.auditReason || "-" }}</b></div>
        </div>
        <div v-if="withdrawDetail" class="proof-grid">
          <div>
            <b>收款码</b>
            <img v-if="withdrawDetail.qrCodeUrl" :src="fileUrl(withdrawDetail.qrCodeUrl)" alt="" />
            <p v-else>未上传</p>
          </div>
          <div>
            <b>打款凭证</b>
            <img v-if="withdrawDetail.paidProofUrl" :src="fileUrl(withdrawDetail.paidProofUrl)" alt="" />
            <p v-else>暂无凭证</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="composerOpen" class="overlay" @click.self="composerOpen = false">
      <div class="sheet">
        <button class="close" @click="composerOpen = false"><X :size="18" /></button>
        <h2>发布动态</h2>
        <textarea v-model="postContent" maxlength="500" placeholder="分享一个想法、任务线索或本地动态"></textarea>
        <div class="preview-grid"><img v-for="img in postImages" :key="img" :src="fileUrl(img)" /></div>
        <label class="upload-btn"><ImagePlus :size="18" />添加图片<input type="file" accept="image/*" multiple @change="onPostImage" /></label>
        <button class="primary full" @click="publishPost"><Send :size="18" />发布</button>
      </div>
    </div>

    <div v-if="commentsOpen" class="overlay" @click.self="commentsOpen = false">
      <div class="sheet">
        <button class="close" @click="commentsOpen = false"><X :size="18" /></button>
        <h2>评论</h2>
        <div class="comments">
          <div v-for="c in comments" :key="c.id" class="comment">
            <div class="comment-avatar" :class="{ 'ai-comment-avatar': isAiComment(c.userName) }">
              <img v-if="c.userAvatar" :src="fileUrl(c.userAvatar)" alt="" />
              <span v-else>{{ c.userName?.slice(0, 1) || "匿" }}</span>
            </div>
            <div><b>{{ c.userName }}</b><p>{{ c.content }}</p><span>{{ fmtTime(c.createdAt) }}</span></div>
          </div>
          <div v-if="!comments.length" class="empty">暂无评论</div>
        </div>
        <div class="comment-box"><input v-model="commentText" placeholder="写评论..." /><button @click="submitComment"><Send :size="17" /></button></div>
      </div>
    </div>

    <div v-if="postDetailOpen" class="overlay" @click.self="postDetailOpen = false">
      <div class="sheet">
        <button class="close" @click="postDetailOpen = false"><X :size="18" /></button>
        <h2>动态详情</h2>
        <article v-if="detailPost" class="post-card detail-post">
          <div class="post-head">
            <button class="avatar plain" @click="openUserCard(detailPost.authorId)">
              <img v-if="detailPost.authorAvatar" :src="fileUrl(detailPost.authorAvatar)" alt="" />
              <span v-else>{{ avatarText(detailPost.authorName) }}</span>
            </button>
            <div>
              <div class="name">{{ detailPost.authorName || "匿名用户" }}</div>
              <div class="meta">{{ plazaMetaText(detailPost) }}</div>
            </div>
          </div>
          <p class="post-text">{{ detailPost.content || "图片动态" }}</p>
          <div v-if="detailPost.images?.length" class="image-grid" :class="'cols-' + Math.min(3, detailPost.images.length)">
            <img v-for="img in detailPost.images" :key="img" :src="fileUrl(img)" alt="" />
          </div>
          <div v-if="detailPost.music" class="music-embed" :class="{ deleted: musicAttachmentDeleted(detailPost.music) }">
            <div class="music-embed-head">
              <img v-if="detailPost.music.imageUrl" :src="fileUrl(detailPost.music.imageUrl)" alt="" />
              <Music2 v-else :size="22" />
              <div>
                <b>{{ detailPost.music.title }}</b>
                <span v-if="musicAttachmentDeleted(detailPost.music)">作者已删除音乐</span>
                <span v-else>作者 {{ detailPost.music.authorName || detailPost.authorName }} · 作曲 {{ detailPost.music.composer || "AI音乐生成" }} · 作词 {{ detailPost.music.lyricist || "AI作词" }}</span>
              </div>
            </div>
            <button v-if="musicAttachmentReady(detailPost.music)" class="player-inline-play" @click="playSong({ ...detailPost.music, authorName: detailPost.music.authorName || detailPost.authorName, createdAt: detailPost.createdAt })">
              <Pause v-if="isSongPlaying(detailPost.music)" :size="16" /><Play v-else :size="16" />{{ playButtonLabel({ ...detailPost.music, authorName: detailPost.music.authorName || detailPost.authorName }) }}
            </button>
          </div>
          <div class="action-row">
            <button :class="{ liked: detailPost.liked }" @click="toggleLike(detailPost)"><Heart :size="18" />{{ detailPost.likeCount }}</button>
            <button @click="openComments(detailPost)"><MessageCircle :size="18" />{{ detailPost.commentCount }}</button>
            <button @click="sharePost(detailPost)"><Share2 :size="18" />分享</button>
          </div>
        </article>
        <div class="comments">
          <div v-for="c in detailComments" :key="c.id" class="comment">
            <div class="comment-avatar" :class="{ 'ai-comment-avatar': isAiComment(c.userName) }">
              <img v-if="c.userAvatar" :src="fileUrl(c.userAvatar)" alt="" />
              <span v-else>{{ c.userName?.slice(0, 1) || "匿" }}</span>
            </div>
            <div><b>{{ c.userName }}</b><p>{{ c.content }}</p><span>{{ fmtTime(c.createdAt) }}</span></div>
          </div>
          <div v-if="!detailComments.length" class="empty">暂无评论</div>
        </div>
      </div>
    </div>

    <div v-if="publishOpen" class="overlay" @click.self="publishOpen = false">
      <div class="sheet">
        <button class="close" @click="publishOpen = false"><X :size="18" /></button>
        <h2>发布任务</h2>
        <div class="form">
          <input v-model="taskForm.title" placeholder="任务标题" />
          <textarea v-model="taskForm.content" placeholder="任务说明"></textarea>
          <input v-model="taskForm.locationText" placeholder="地点/方式，例如线上、本地" />
          <input v-model="taskForm.amount" type="number" min="0.01" step="0.01" placeholder="奖励金额" />
          <input v-model.number="taskForm.totalSlots" type="number" min="1" placeholder="人数" />
          <input v-model="taskForm.deadlineAt" type="datetime-local" />
          <input v-model="taskForm.proofRequirements" placeholder="凭证要求" />
          <button class="accent full" @click="publishTask">提交审核</button>
        </div>
      </div>
    </div>

    <div v-if="rechargeOpen" class="overlay" @click.self="closeRecharge">
      <div class="sheet">
        <button class="close" @click="closeRecharge"><X :size="18" /></button>
        <h2>微信充值</h2>
        <div class="form">
          <input v-if="!rechargeQrDataUrl" v-model="rechargeAmount" type="number" min="0.01" step="0.01" placeholder="充值金额" />
          <template v-if="rechargeConfig?.channel === 'WXPAY_NATIVE'">
            <div v-if="rechargeQrDataUrl" style="text-align:center">
              <p style="margin-bottom:8px;font-size:13px;color:#666">请使用微信扫码完成支付，正在等待支付结果…</p>
              <img :src="rechargeQrDataUrl" style="width:200px;height:200px" />
            </div>
            <button v-else class="accent full" @click="completeRecharge">生成支付二维码</button>
          </template>
          <template v-else>
            <div class="info-box"><b>{{ rechargeConfig?.name || "平台收款" }}</b><p>目前仅支持微信扫码支付，支付完成后点下方按钮确认。</p></div>
            <div v-if="rechargeConfig?.qrCodeUrl" class="preview-grid single">
              <img :src="fileUrl(rechargeConfig.qrCodeUrl)" />
            </div>
            <div v-else class="empty">后台暂未配置平台收款码</div>
            <button class="accent full" @click="completeRecharge">已完成微信支付</button>
          </template>
        </div>
      </div>
    </div>

    <div v-if="packagePurchaseOpen" class="overlay" @click.self="packagePurchaseOpen = false">
      <div class="sheet">
        <button class="close" @click="packagePurchaseOpen = false"><X :size="18" /></button>
        <h2>购买音乐套餐</h2>
        <div class="info-box">
          <b>{{ selectedMusicPackage?.name }}</b>
          <p>{{ selectedMusicPackage?.credits }} 次 AI 音乐生成 · 原价 ¥{{ selectedMusicPackage?.originalPrice }} · {{ selectedMusicPackage?.discountText }}</p>
        </div>
        <div class="stat-card">
          <WalletCards :size="20" />
          <b>¥{{ selectedMusicPackage?.price || "0.00" }}</b>
          <span>将从可用余额抵扣</span>
        </div>
        <button class="accent full" @click="buyMusicPackage">确认购买</button>
      </div>
    </div>

    <div v-if="taskDetailOpen" class="overlay" @click.self="taskDetailOpen = false">
      <div class="sheet">
        <button class="close" @click="taskDetailOpen = false"><X :size="18" /></button>
        <h2>{{ taskDetail?.title || "任务详情" }}</h2>
        <div class="detail-meta">
          <span class="badge soft">{{ taskDetail?.category || "轻任务" }}</span>
          <span>{{ taskDetail?.taskNo }}</span>
          <span>{{ statusText(taskDetail?.status) }}</span>
          <b>¥{{ taskDetail?.amount || "0.00" }}</b>
          <span>{{ taskDetail?.acceptedSlots || 0 }}/{{ taskDetail?.totalSlots || 0 }} 人</span>
        </div>
        <p class="detail-text">{{ taskDetail?.content }}</p>
        <div class="info-box">
          <b>凭证要求</b>
          <p>{{ taskDetail?.proofRequirements || "按任务说明提交" }}</p>
          <span>地点：{{ taskDetail?.locationText || "线上" }} · 截止 {{ fmtTime(taskDetail?.deadlineAt) }}</span>
        </div>
        <div v-if="myOrder" class="info-box">
          <b>我的接单</b>
          <p>{{ myOrder.orderNo }} · {{ statusText(myOrder.orderStatus) }} · 接单 {{ fmtTime(myOrder.acceptTime) }} · 提交 {{ fmtTime(myOrder.submitTime) }}</p>
        </div>
        <div class="submissions">
          <h3>完成记录</h3>
          <article v-for="s in taskSubmissions" :key="s.orderNo" class="submission-card">
            <div class="post-head">
              <div class="avatar small">
                <img v-if="s.avatar" :src="fileUrl(s.avatar)" alt="" />
                <span v-else>{{ avatarText(s.displayName) }}</span>
              </div>
              <div>
                <b>{{ s.displayName }}</b>
                <p>
                  <span class="submission-status" :class="submissionStatusClass(s.orderStatus)">{{ statusText(s.orderStatus) }}</span>
                  <span>{{ fmtTime(s.submitTime) }}</span>
                  <template v-if="s.orderStatus === 'SETTLED' && s.settledAmount"> · 结算 ¥{{ s.settledAmount }}</template>
                </p>
              </div>
              <button :class="{ liked: s.likedByMe }" @click="toggleSubmissionLike(s)"><Heart :size="16" />{{ s.likeCount }}</button>
            </div>
            <div class="proof-list">
              <template v-for="p in s.proofs" :key="p.type + p.url + p.remark">
                <img v-if="p.type === 'IMAGE'" :src="fileUrl(p.url)" alt="" />
                <p v-else>{{ p.remark }}</p>
              </template>
            </div>
          </article>
          <div v-if="!taskSubmissions.length" class="empty">暂无完成记录</div>
        </div>
        <button v-if="!myOrder" class="primary full" @click="acceptTask(taskDetail)">立即接单</button>
        <button v-else-if="['ACCEPTED', 'REJECTED_RESUBMIT'].includes(myOrder.orderStatus)" class="accent full" @click="submitProofOpen = true">提交任务凭证</button>
      </div>
    </div>

    <div v-if="submitProofOpen" class="overlay" @click.self="submitProofOpen = false">
      <div class="sheet">
        <button class="close" @click="submitProofOpen = false"><X :size="18" /></button>
        <h2>提交凭证</h2>
        <textarea v-model="proofText" placeholder="填写完成说明、截图说明或联系信息"></textarea>
        <div v-if="proofImages.length" class="preview-grid"><img v-for="img in proofImages" :key="img" :src="fileUrl(img)" /></div>
        <label class="upload-btn"><ImagePlus :size="18" />上传图片凭证<input type="file" accept="image/*" multiple @change="onProofImage" /></label>
        <button class="accent full" @click="submitOrderProof">提交审核</button>
      </div>
    </div>

    <div v-if="editProfileOpen" class="overlay" @click.self="editProfileOpen = false">
      <div class="sheet">
        <button class="close" @click="editProfileOpen = false"><X :size="18" /></button>
        <h2>编辑资料</h2>
        <div class="form">
          <div class="profile-preview">
            <div class="profile-avatar">
              <img v-if="profileForm.avatar" :src="fileUrl(profileForm.avatar)" alt="" />
              <span v-else>{{ avatarText(profileForm.nickname) }}</span>
            </div>
            <label class="upload-btn"><ImagePlus :size="18" />更换头像<input type="file" accept="image/*" @change="onProfileAvatar" /></label>
          </div>
          <input v-model="profileForm.nickname" placeholder="昵称" />
          <select v-model="profileForm.gender">
            <option value="UNKNOWN">不展示</option>
            <option value="MALE">男生</option>
            <option value="FEMALE">女生</option>
          </select>
          <textarea v-model="profileForm.signature" maxlength="255" placeholder="个性签名"></textarea>
          <button class="primary full" @click="saveProfile">保存资料</button>
        </div>
      </div>
    </div>

    <div v-if="withdrawOpen" class="overlay" @click.self="withdrawOpen = false">
      <div class="sheet">
        <button class="close" @click="withdrawOpen = false"><X :size="18" /></button>
        <h2>申请提现</h2>
        <div class="form">
          <input v-model="withdrawForm.amount" type="number" min="0.01" step="0.01" placeholder="提现金额" />
          <div class="info-box"><b>提现方式</b><p>仅支持微信收款码</p></div>
          <div v-if="withdrawForm.qrCodeUrl || wallet?.withdrawQrCodeUrl" class="preview-grid single">
            <img :src="fileUrl(withdrawForm.qrCodeUrl || wallet?.withdrawQrCodeUrl)" />
          </div>
          <label class="upload-btn"><QrCode :size="18" />上传/更新收款码<input type="file" accept="image/*" @change="onWithdrawQr" /></label>
          <button class="accent full" @click="applyWithdraw">提交提现</button>
        </div>
      </div>
    </div>

    <div v-if="feedbackOpen" class="overlay" @click.self="feedbackOpen = false">
      <div class="sheet">
        <button class="close" @click="feedbackOpen = false"><X :size="18" /></button>
        <h2>反馈建议</h2>
        <div class="form">
          <textarea v-model="feedbackForm.content" maxlength="1000" placeholder="描述你遇到的问题或建议"></textarea>
          <input v-model="feedbackForm.contact" maxlength="128" placeholder="联系方式，可选" />
          <button class="primary full" @click="submitFeedback">提交反馈</button>
        </div>
      </div>
    </div>

    <div v-if="userCardOpen" class="overlay" @click.self="userCardOpen = false">
      <div class="sheet">
        <button class="close" @click="userCardOpen = false"><X :size="18" /></button>
        <div class="profile-card mini">
          <div class="profile-avatar">
            <img v-if="userCard?.avatar" :src="fileUrl(userCard.avatar)" alt="" />
            <span v-else>{{ avatarText(userCard?.nickname) }}</span>
          </div>
          <div>
            <h1>{{ userCard?.nickname || "用户主页" }}</h1>
            <p>ID {{ userCard?.userId }} · 动态 {{ userCard?.postCount }} · 关注 {{ userCard?.followingCount }} · 粉丝 {{ userCard?.followerCount }}</p>
            <p v-if="userCard?.signature">{{ userCard.signature }}</p>
          </div>
          <button v-if="userCard && !userCard.self" class="primary small" @click="toggleFollowUser()">{{ userCard.followed ? "取消关注" : "关注" }}</button>
        </div>
        <template v-if="userCardView === 'home'">
          <div class="pill-row user-card-tabs">
            <button :class="{ active: userCardContentTab === 'posts' }" @click="userCardContentTab = 'posts'">动态 {{ userCardPosts.length }}</button>
            <button :class="{ active: userCardContentTab === 'tasks' }" @click="userCardContentTab = 'tasks'">任务 {{ userCardTasks.length }}</button>
            <button :class="{ active: userCardContentTab === 'music' }" @click="userCardContentTab = 'music'">音乐 {{ userCardMusic.length }}</button>
          </div>

          <template v-if="userCardContentTab === 'posts'">
            <article v-for="post in userCardPosts" :key="post.id" class="post-card compact">
              <div class="meta">{{ fmtTime(post.createdAt) }} · {{ post.categoryName || post.category }}</div>
              <p class="post-text">{{ post.content || "图片动态" }}</p>
              <div v-if="post.images?.length" class="image-grid" :class="'cols-' + Math.min(3, post.images.length)">
                <img v-for="img in post.images" :key="img" :src="fileUrl(img)" alt="" />
              </div>
              <div v-if="post.music" class="music-embed" :class="{ deleted: musicAttachmentDeleted(post.music) }">
                <div class="music-embed-head">
                  <img v-if="post.music.imageUrl" :src="fileUrl(post.music.imageUrl)" alt="" />
                  <Music2 v-else :size="22" />
                  <div>
                    <b>{{ post.music.title }}</b>
                    <span v-if="musicAttachmentDeleted(post.music)">作者已删除音乐</span>
                    <span v-else>作者 {{ post.music.authorName || post.authorName }} · 作曲 {{ post.music.composer || "AI音乐生成" }} · 作词 {{ post.music.lyricist || "AI作词" }}</span>
                  </div>
                </div>
                <button v-if="musicAttachmentReady(post.music)" class="player-inline-play" @click.stop="playSong({ ...post.music, authorName: post.music.authorName || post.authorName, createdAt: post.createdAt })">
                  <Pause v-if="isSongPlaying(post.music)" :size="16" /><Play v-else :size="16" />{{ playButtonLabel({ ...post.music, authorName: post.music.authorName || post.authorName }) }}
                </button>
              </div>
              <div class="action-row">
                <button :class="{ liked: post.liked }" @click="toggleLike(post)"><Heart :size="18" />{{ post.likeCount }}</button>
                <button @click="toggleUserPostComments(post)"><MessageCircle :size="18" />{{ userPostCommentsOpen[post.id] ? "收起" : post.commentCount }}</button>
                <button @click.stop="sharePost(post)"><Share2 :size="18" />分享</button>
              </div>
              <div v-if="userPostCommentsOpen[post.id]" class="comments inline-comments">
                <div v-for="c in userPostComments[post.id] || []" :key="c.id" class="comment">
                  <div class="comment-avatar" :class="{ 'ai-comment-avatar': isAiComment(c.userName) }">
                    <img v-if="c.userAvatar" :src="fileUrl(c.userAvatar)" alt="" />
                    <span v-else>{{ c.userName?.slice(0, 1) || "匿" }}</span>
                  </div>
                  <div><b>{{ c.userName }}</b><p>{{ c.content }}</p><span>{{ fmtTime(c.createdAt) }}</span></div>
                </div>
                <div v-if="!(userPostComments[post.id] || []).length" class="empty">暂无评论</div>
                <div class="comment-box"><input v-model="userPostCommentText[post.id]" placeholder="写评论..." /><button @click="submitUserPostComment(post)"><Send :size="17" /></button></div>
              </div>
            </article>
            <div v-if="!userCardPosts.length" class="empty">暂无公开动态</div>
          </template>

          <template v-if="userCardContentTab === 'tasks'">
            <article v-for="task in userCardTasks" :key="task.taskNo" class="list-card" @click="userCardOpen = false; openTaskDetail(task)">
              <div><b>{{ task.title }}</b><p>{{ task.taskNo }} · ¥{{ task.amount }} · {{ task.acceptedSlots }}/{{ task.totalSlots }}人 · 截止 {{ fmtTime(task.deadlineAt) }}</p></div>
              <span>{{ statusText(task.myOrderStatus || 'PUBLISHED') }}</span>
            </article>
            <div v-if="!userCardTasks.length" class="empty">暂无公开任务</div>
          </template>

          <template v-if="userCardContentTab === 'music'">
            <article v-for="song in userCardMusic" :key="song.id" class="music-card">
              <div class="music-cover">
                <img v-if="song.imageUrl" :src="fileUrl(song.imageUrl)" alt="" />
                <Music2 v-else :size="28" />
              </div>
              <div class="music-main">
                <div class="name">{{ song.title }}</div>
                <div class="meta">作者 {{ song.authorName || userCard?.nickname || "匿名用户" }} · 作曲 {{ song.composer || "AI音乐生成" }} · 作词 {{ song.lyricist || (song.instrumental ? "纯音乐" : "AI作词") }}</div>
                <div class="meta">{{ song.ratingCount || 0 }}人点赞 · 打赏 ¥{{ song.tipTotal || "0.00" }} · {{ fmtTime(song.createdAt) }}</div>
                <button v-if="musicReady(song)" class="player-inline-play" @click="playSong(song)">
                  <Pause v-if="isSongPlaying(song)" :size="16" /><Play v-else :size="16" />{{ playButtonLabel(song) }}
                </button>
                <details v-if="song.lyrics || song.prompt" class="music-lyrics-box compact">
                  <summary>歌词</summary>
                  <p>{{ song.lyrics || song.prompt }}</p>
                </details>
                <div class="action-row">
                  <button :class="{ liked: song.liked }" @click="likeUserMusic(song)"><Heart :size="18" />{{ song.ratingCount || 0 }}</button>
                  <button @click="toggleUserMusicComments(song)"><MessageCircle :size="18" />{{ userMusicCommentsOpen[song.id] ? "收起" : "评论" }}</button>
                  <button @click="shareUserMusic(song)"><Share2 :size="18" />分享</button>
                </div>
                <div v-if="userMusicCommentsOpen[song.id]" class="comments inline-comments">
                  <div v-for="c in userMusicComments[song.id] || []" :key="c.id" class="comment">
                    <div class="comment-avatar" :class="{ 'ai-comment-avatar': isAiComment(c.userName) }">
                      <img v-if="c.userAvatar" :src="fileUrl(c.userAvatar)" alt="" />
                      <span v-else>{{ c.userName?.slice(0, 1) || "匿" }}</span>
                    </div>
                    <div><b>{{ c.userName }}</b><p>{{ c.content }}</p><span>{{ fmtTime(c.createdAt) }}</span></div>
                  </div>
                  <div v-if="!(userMusicComments[song.id] || []).length" class="empty">暂无评论</div>
                  <div class="comment-box"><input v-model="userMusicCommentText[song.id]" placeholder="写评论..." /><button @click="submitUserMusicComment(song)"><Send :size="17" /></button></div>
                </div>
              </div>
            </article>
            <div v-if="!userCardMusic.length" class="empty">暂无公开音乐</div>
          </template>
        </template>

        <template v-else>
          <button class="ghost small" @click="userCardView = 'home'">返回主页</button>
          <article v-if="userCardDetailPost" class="post-card detail-post">
            <div class="meta">{{ plazaMetaText(userCardDetailPost) }}</div>
            <p class="post-text">{{ userCardDetailPost.content || "图片动态" }}</p>
            <div v-if="userCardDetailPost.images?.length" class="image-grid" :class="'cols-' + Math.min(3, userCardDetailPost.images.length)">
              <img v-for="img in userCardDetailPost.images" :key="img" :src="fileUrl(img)" alt="" />
            </div>
            <div class="action-row">
              <button :class="{ liked: userCardDetailPost.liked }" @click="toggleLike(userCardDetailPost)"><Heart :size="18" />{{ userCardDetailPost.likeCount }}</button>
              <button><MessageCircle :size="18" />{{ userCardDetailPost.commentCount }}</button>
              <button @click="sharePost(userCardDetailPost)"><Share2 :size="18" />分享</button>
            </div>
          </article>
          <div class="comments">
            <div v-for="c in userCardDetailComments" :key="c.id" class="comment">
              <div class="comment-avatar" :class="{ 'ai-comment-avatar': isAiComment(c.userName) }">
                <img v-if="c.userAvatar" :src="fileUrl(c.userAvatar)" alt="" />
                <span v-else>{{ c.userName?.slice(0, 1) || "匿" }}</span>
              </div>
              <div><b>{{ c.userName }}</b><p>{{ c.content }}</p><span>{{ fmtTime(c.createdAt) }}</span></div>
            </div>
            <div v-if="!userCardDetailComments.length" class="empty">暂无评论</div>
          </div>
          <div class="comment-box"><input v-model="userCardCommentText" placeholder="写评论..." /><button @click="submitUserCardComment"><Send :size="17" /></button></div>
        </template>
      </div>
    </div>

    <div v-if="playerOpen && currentSong" class="music-player" :class="{ expanded: playerExpanded, playing: playerPlaying }">
      <audio
        ref="playerAudio"
        class="player-audio-core"
        :src="fileUrl(currentSong.audioUrl)"
        @loadedmetadata="onPlayerMeta"
        @timeupdate="onPlayerTime"
        @play="playerPlaying = true"
        @pause="playerPlaying = false"
        @ended="onPlayerEnded"
      ></audio>

      <button v-if="playerExpanded" class="player-collapse" title="收起" @click="playerExpanded = false">
        <ChevronDown :size="24" />
      </button>
      <div v-if="playerExpanded" class="player-now">
        <span></span>
        <b>正在播放</b>
      </div>

      <div class="player-shell">
        <div class="player-track">
          <div class="player-cover">
            <img v-if="currentSong.imageUrl" :src="fileUrl(currentSong.imageUrl)" alt="" />
            <Music2 v-else :size="24" />
          </div>
          <div class="player-info">
            <b>{{ currentSong.title }}</b>
            <span>{{ currentSong.authorName || "匿名用户" }}</span>
          </div>
        </div>

        <div class="player-controls">
          <button title="上一首" @click="playAdjacentSong(-1)"><SkipBack :size="19" /></button>
          <button class="player-main-btn" title="播放/暂停" @click="togglePlayerPlay">
            <Pause v-if="playerPlaying" :size="22" />
            <Play v-else :size="22" />
          </button>
          <button title="下一首" @click="playAdjacentSong(1)"><SkipForward :size="19" /></button>
        </div>

        <div class="player-tools">
          <button title="播放列表" @click="playerListOpen = !playerListOpen"><ListMusic :size="19" /></button>
          <button title="全屏" @click="playerExpanded = true"><Maximize2 :size="18" /></button>
          <button :title="playerExpanded ? '退出全屏' : '关闭'" @click="playerExpanded ? (playerExpanded = false) : closePlayer()"><Minus :size="19" /></button>
        </div>

        <div class="player-progress">
          <span>{{ fmtDuration(playerCurrentTime) }}</span>
          <input
            type="range"
            min="0"
            :max="playerDuration || 0"
            step="0.1"
            :value="playerCurrentTime"
            :style="{ '--progress': playerProgress + '%' }"
            @input="seekPlayer"
          />
          <span>{{ fmtDuration(playerDuration) }}</span>
        </div>
      </div>

      <div v-if="playerExpanded" class="player-full">
        <div class="player-full-cover">
          <img v-if="currentSong.imageUrl" :src="fileUrl(currentSong.imageUrl)" alt="" />
          <Music2 v-else :size="64" />
        </div>
        <div class="player-full-info">
          <h2>{{ currentSong.title }}</h2>
          <p class="player-author">{{ currentSong.authorName || "匿名用户" }}</p>
          <p class="player-credit strong">作词：{{ currentSong.lyricist || currentSong.authorName || "AI作词" }}</p>
          <p class="player-credit">作曲：{{ currentSong.composer || currentSong.authorName || "AI音乐生成" }}</p>
          <div v-if="playerLyricLines.length" ref="playerLyricsRef" class="player-lyrics">
            <p
              v-for="(line, index) in playerLyricLines"
              :key="index + line"
              :class="{ active: index === activeLyricIndex, passed: index < activeLyricIndex }"
              :data-active-lyric="index === activeLyricIndex ? 'true' : 'false'"
            >
              {{ line }}
            </p>
          </div>
          <div v-else class="player-lyrics empty-lyrics">暂无歌词</div>
        </div>
      </div>

      <div v-if="playerListOpen" class="player-list">
        <button
          v-for="song in playerQueue"
          :key="song.id"
          :class="{ active: currentSong.id === song.id }"
          @click="playSong(song)"
        >
          <img v-if="song.imageUrl" :src="fileUrl(song.imageUrl)" alt="" />
          <Music2 v-else :size="18" />
          <span>{{ song.title }}</span>
          <small>{{ song.authorName || "匿名用户" }}</small>
        </button>
      </div>
    </div>

    <div v-if="loading || refreshing" class="loading"><Loader2 class="spin" :size="18" />{{ refreshing ? "刷新中" : "加载中" }}</div>
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>
