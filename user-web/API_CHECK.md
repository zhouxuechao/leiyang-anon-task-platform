# H5/PC 用户端接口对接清单

依据：`http://127.0.0.1:8080/v3/api-docs`，仅统计 `/api/mp/**` 与 `/api/common/**`。

## 已对接

| 接口 | 用途 |
| --- | --- |
| `GET /api/mp/home` | 首页 banner、公告 |
| `POST /api/mp/auth/phone/send-code` | 手机号验证码 |
| `POST /api/mp/auth/phone-login` | 手机号注册/登录 |
| `GET /api/mp/auth/me` | 我的资料 |
| `POST /api/mp/auth/me` | 编辑昵称、头像、性别、签名 |
| `POST /api/common/upload` | 头像、动态图片、凭证、收款码上传 |
| `GET /api/mp/public/plaza/meta` | 广场排序、分类、AI provider |
| `GET /api/mp/public/plaza/posts` | 广场分页列表 |
| `POST /api/mp/plaza/posts` | 发布动态 |
| `POST /api/mp/plaza/posts/{postId}/like` | 点赞动态 |
| `DELETE /api/mp/plaza/posts/{postId}/like` | 取消点赞 |
| `GET /api/mp/public/plaza/posts/{postId}/comments` | 评论列表 |
| `POST /api/mp/plaza/posts/{postId}/comments` | 发布评论 |
| `GET /api/mp/plaza/my-posts` | 我的动态分页 |
| `GET /api/mp/public/plaza/users/{userId}/card` | 用户主页卡片 |
| `GET /api/mp/public/plaza/users/{userId}/posts` | 用户主页动态 |
| `POST /api/mp/plaza/follow/{targetUserId}` | 关注用户 |
| `DELETE /api/mp/plaza/follow/{targetUserId}` | 取消关注 |
| `GET /api/mp/plaza/follows/summary` | 关注/粉丝统计 |
| `GET /api/mp/plaza/follows/following` | 关注列表 |
| `GET /api/mp/plaza/follows/followers` | 粉丝列表 |
| `GET /api/mp/tasks` | 任务分页列表 |
| `POST /api/mp/tasks` | 发布任务 |
| `GET /api/mp/tasks/{taskNo}` | 任务详情 |
| `POST /api/mp/tasks/{taskNo}/accept` | 接单 |
| `GET /api/mp/tasks/{taskNo}/my-order` | 当前用户接单状态 |
| `GET /api/mp/public/tasks/{taskNo}/submissions` | 公开完成记录 |
| `GET /api/mp/tasks/{taskNo}/submissions` | 登录态完成记录 |
| `POST /api/mp/tasks/{taskNo}/submissions/{orderNo}/like` | 完成记录点赞/取消 |
| `POST /api/mp/orders/{orderNo}/submit` | 提交文字/图片凭证 |
| `GET /api/mp/orders/my` | 我的接单分页 |
| `GET /api/mp/wallet` | 钱包余额、冻结、累计收益、收款码 |
| `POST /api/mp/wallet/withdraw-qr` | 保存收款码 |
| `POST /api/mp/wallet/withdraw` | 申请提现 |
| `GET /api/mp/wallet/my-withdraws` | 提现记录 |
| `GET /api/mp/messages` | 消息列表 |
| `POST /api/mp/messages/{id}/read` | 标记已读 |
| `POST /api/mp/feedbacks` | 反馈建议 |

## 未直接调用

| 接口 | 原因 |
| --- | --- |
| `POST /api/mp/auth/wx-login` | 当前接口是小程序 `wx.login` code 登录，不适用于普通 H5/PC 网页；H5/PC 登录页已只保留手机号登录。 |
| `POST /api/mp/auth/mock-login` | 开发兜底接口，正式 H5/PC 用户端不展示。 |

## 分页与刷新

- 广场动态、任务大厅、我的动态、我的接单均使用 `page + size` 分页参数。
- 页面滚动接近底部时自动请求下一页。
- 移动端在页面顶部下拉会刷新当前视图；筛选、搜索、发布、接单、提交凭证后也会刷新相关列表。
