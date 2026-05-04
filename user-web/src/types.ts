export type User = {
  id: number;
  openId: string;
  nickname: string;
  avatar?: string;
  gender?: string;
  signature?: string;
  status: string;
  creditScore: number;
};

export type PlazaPost = {
  id: number;
  authorId: number;
  authorName: string;
  authorAvatar?: string;
  gender?: string;
  category: string;
  categoryName?: string;
  content: string;
  images: string[];
  likeCount: number;
  commentCount: number;
  liked: boolean;
  followed: boolean;
  hot?: boolean;
  createdAt: string;
  music?: MusicAttachment;
};

export type MusicAttachment = {
  id: number;
  title: string;
  audioUrl?: string;
  imageUrl?: string;
  videoUrl?: string;
  duration?: string;
  authorName?: string;
  lyricist?: string;
  composer?: string;
  lyrics?: string;
  style?: string;
  deleted?: boolean;
};

export type PlazaMeta = {
  sorts: { code: string; name: string }[];
  categories: { code: string; name: string }[];
  aiProviders: { code: string; name: string; abbr: string; logoText: string; logoUrl?: string }[];
};

export type HomeData = {
  banners: { id: number; imageUrl: string; linkUrl?: string }[];
  notices: { id: number; title: string; content: string }[];
  navHotTabs?: string[];
};

export type SlicePage<T> = {
  items: T[];
  page: number;
  size: number;
  hasMore: boolean;
};

export type UserCard = {
  userId: number;
  nickname: string;
  avatar?: string;
  signature?: string;
  postCount: number;
  followingCount: number;
  followerCount: number;
  followed: boolean;
  self: boolean;
};

export type CommentItem = {
  id: number;
  userId: number;
  userName: string;
  userAvatar?: string;
  content: string;
  createdAt: string;
};

export type TaskItem = {
  taskNo: string;
  title: string;
  category: string;
  amount: string;
  locationText: string;
  totalSlots: number;
  acceptedSlots: number;
  createdAt: string;
  deadlineAt: string;
  status?: string;
  myOrderNo?: string;
  myOrderStatus?: string;
};

export type TaskDetail = TaskItem & {
  content: string;
  proofRequirements?: string;
  status: string;
};

export type TaskSubmission = {
  orderNo: string;
  displayName: string;
  avatar?: string;
  orderStatus: string;
  submitTime?: string;
  settledAmount?: string;
  likeCount: number;
  likedByMe: boolean;
  proofs: { type: string; url: string; remark?: string }[];
};

export type OrderItem = {
  orderNo: string;
  taskNo: string;
  taskTitle: string;
  amount: string;
  orderStatus: string;
  acceptTime: string;
  submitTime?: string;
  auditReason?: string;
};

export type MessageItem = {
  id: number;
  type: string;
  title: string;
  content: string;
  read: boolean;
  createdAt: string;
};

export type WithdrawItem = {
  applyNo: string;
  amount: string;
  channel: string;
  status: string;
  auditReason?: string;
  createdAt: string;
  qrCodeUrl?: string;
  paidProofUrl?: string;
  payRemark?: string;
  paidAt?: string;
};

export type FollowSummary = {
  followingCount: number;
  followerCount: number;
};

export type FollowUser = {
  userId: number;
  nickname: string;
  avatar?: string;
  signature?: string;
  postCount: number;
  followed: boolean;
};

export type Wallet = {
  balance: string;
  frozenAmount: string;
  totalIncome: string;
  withdrawQrCodeUrl?: string;
};

export type WalletFlow = {
  type: string;
  amount: string;
  bizNo?: string;
  status: string;
  createdAt: string;
  label: string;
};

export type RechargeConfig = {
  channel: string;
  qrCodeUrl?: string;
  name?: string;
};

export type MusicJob = {
  id: number;
  title: string;
  prompt: string;
  style?: string;
  customMode: boolean;
  instrumental: boolean;
  lang?: string;
  status: string;
  sunoTaskId?: string;
  audioUrl?: string;
  videoUrl?: string;
  imageUrl?: string;
  duration?: string;
  published: boolean;
  rating?: number;
  ratingCount: number;
  liked: boolean;
  tipTotal: string;
  plazaPostId?: number;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
  authorId?: number;
  authorName?: string;
  authorAvatar?: string;
  lyricist?: string;
  composer?: string;
  lyrics?: string;
};

export type MusicCredits = {
  remaining: number;
  total: number;
  used: number;
  freeWeek: boolean;
  dailyFreeRemaining: number;
  dailyFreeTotal: number;
  packageRemaining: number;
  paidPrice: string;
};

export type MusicPackage = {
  code: string;
  name: string;
  credits: number;
  price: string;
  originalPrice: string;
  discountText: string;
};
