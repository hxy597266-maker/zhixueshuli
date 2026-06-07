// ==================== 公共 JavaScript common.js ====================

// 显示 loading 动画
function showLoading(id){
    document.getElementById(id).innerHTML = `
        <div class="loading">
            <div class="loading-spinner"></div>
        </div>
    `;
}

// 1. 加载用户信息（导航栏显示）
async function loadUserInfo() {
    try {
        const res = await fetchWithAuth('/user/info');
        
        if (res.status === 401) {
            // 未登录，显示默认状态
            const loginNav = document.getElementById('loginNav');
            if (loginNav) {
                loginNav.innerHTML = `
                    <a href="login.html">登录</a>
                    <a href="register.html">注册</a>
                `;
            }
            return;
        }
        
        const data = await res.json();
        if (data.code === 200 && data.data) {
            const user = data.data;
            const username = user.username || '未登录';
            const avatar = user.avatar || '/images/default-avatar.png';
            
            // 更新导航栏用户信息
            const loginNav = document.getElementById('loginNav');
            if (loginNav) {
                loginNav.innerHTML = `
                    <a href="profile.html">${username}</a>
                    <a href="javascript:logout()">退出</a>
                `;
            }
        }
    } catch (error) {
        console.error('加载用户信息失败:', error);
    }
}

// 2. 渲染帖子列表
function renderPostList(list) {
    if (!list || list.length === 0) {
        const listEl = document.getElementById("list");
        if (listEl) {
            listEl.innerHTML = `
                <div style="text-align: center; color: #999; padding: 60px 20px;">
                    <div style="font-size: 48px; margin-bottom: 10px;"></div>
                    <div style="font-size: 16px;">暂无帖子，快来发布第一条吧！</div>
                </div>`;
        }
        return;
    }

    let html = "";
    list.forEach(p => {
        let images = p.image ? p.image.split(",") : [];
        
        // 过滤空字符串
        images = images.filter(img => img && img.trim() !== '');
        
        let imagesHtml = '';
        if (images.length > 0) {
            imagesHtml = images.map(url =>
                `<img src="${url}" class="post-image" onclick="window.open(this.src)">`
            ).join("");
        }

        // 判断用户是否已点赞
        let isLiked = p.likedByCurrentUser ? 'liked' : '';
        let starIcon = p.likedByCurrentUser ? '&#x2B50;' : '&#x2606;';

        // 头像处理：如果有头像显示头像，否则显示首字母
        let avatarHtml = '';
        if (p.userAvatar) {
            avatarHtml = `<img src="${p.userAvatar}" style="width: 100%; height: 100%; object-fit: cover;">`;
        } else {
            avatarHtml = (p.username || '匿')[0];
        }

        // 评论数
        let commentCount = p.commentCount || 0;

        // 计算帖子总长度：文字 + 图片（每张图片折算50字符）
        const imageLength = images.length * 50;
        const textLength = (p.content || '').length;
        const contentLength = textLength + imageLength;
        
        // 调试信息
        console.log(`帖子 ${p.id}: 文字${textLength}字 + ${images.length}张图片(${imageLength}字符) = 总长${contentLength}`);
        
        // 处理帖子内容和图片
        let contentHtml = '';
        let imagesDisplayHtml = '';
        const maxLength = 200;
        
        if (contentLength > maxLength) {
            // 超过限制：截断内容 + 隐藏图片 + 显示详情链接
            const truncatedContent = p.content.substring(0, maxLength);
            
            // 不显示图片
            imagesDisplayHtml = '';
            
            console.log(`→ 超过限制，隐藏图片，显示"查看详情"`);
            
            contentHtml = `
                <div class="post-content" style="margin-bottom: 10px;">${truncatedContent}...</div>
                <a href="post-detail.html?id=${p.id}" style="color: #1890ff; text-decoration: none; font-size: 14px; display: inline-block; margin-bottom: 15px;">
                    查看详情 →
                </a>
            `;
        } else {
            // 未超过限制：显示完整内容 + 所有图片
            contentHtml = `<div class="post-content">${p.content || ''}</div>`;
            imagesDisplayHtml = imagesHtml;
            
            console.log(`→ 未超过限制，显示图片和完整内容`);
        }

        html += `
        <div class="post">
            <div class="post-header">
                <div class="avatar">${avatarHtml}</div>
                <div class="post-meta">
                    <div class="post-author">${p.username || '匿名'}</div>
                    <div class="post-time">${p.createTime || ''}</div>
                </div>
            </div>
            <div class="post-title">
                <a href="post-detail.html?id=${p.id}" style="text-decoration:none;color:#2c3e50;">
                    ${p.title}
                </a>
            </div>
            ${contentHtml}
            ${imagesDisplayHtml}
            <div class="post-actions">
                <div class="action-btn ${isLiked}" onclick="like(${p.id})">
                    <span class="icon" style="font-size: 18px;">${starIcon}</span>
                    <span>${p.likeCount || 0}</span>
                </div>
                <div class="action-btn">
                    <span class="icon" style="font-size: 18px;">&#x1F4AC;</span>
                    <span>${commentCount}</span>
                </div>
            </div>
            <div class="comment-section">
                <div class="comment-input-box">
                    <input id="c${p.id}" placeholder="写下你的评论...">
                    <button onclick="comment(${p.id})">发送</button>
                </div>
                <div id="cmt${p.id}"></div>
            </div>
        </div>`;
    });

    const listEl = document.getElementById("list");
    if (listEl) {
        listEl.innerHTML = html;
        list.forEach(p => loadComments(p.id));
    }
}

// 3. 点赞功能
async function like(pid) {
    const res = await fetchWithAuth('/like/add', {
        method: 'POST',
        body: JSON.stringify({ postId: pid })
    });
    const data = await res.json();
    if (data.code === 200) {
        // 刷新当前页的帖子列表
        const paginationEl = document.getElementById("pagination");
        let currentPage = 1;
        if (paginationEl) {
            const span = paginationEl.querySelector("span");
            if (span) {
                const match = span.textContent.match(/第 (\d+) /);
                if (match) currentPage = parseInt(match[1]);
            }
        }
        loadList(currentPage);
    }
}

// 4. 评论功能
async function comment(pid) {
    let content = document.getElementById("c" + pid).value.trim();
    if (!content) {
        alert("请输入评论内容");
        return;
    }

    const inputEl = document.getElementById("c" + pid);
    const buttonEl = inputEl.nextElementSibling;
    
    // 禁用输入框和按钮，显示 loading 状态
    inputEl.disabled = true;
    buttonEl.disabled = true;
    buttonEl.textContent = "发送中...";

    try {
        const res = await fetchWithAuth('/comment/add', {
            method: 'POST',
            body: JSON.stringify({ postId: pid, content: content })
        });
        
        const data = await res.json();
        if (data.code === 200) {
            // 清空输入框
            inputEl.value = "";
            // 重新加载评论列表（会显示 loading 动画）
            loadComments(pid);
        } else {
            alert(data.msg || "评论失败");
        }
    } catch (error) {
        console.error('评论失败:', error);
        alert("评论出错，请重试");
    } finally {
        // 恢复输入框和按钮状态
        inputEl.disabled = false;
        buttonEl.disabled = false;
        buttonEl.textContent = "发送";
    }
}

// 5. 加载评论
async function loadComments(pid) {
    const res = await fetchWithAuth('/comment/list?postId=' + pid);
    const data = await res.json();
    if (data.code !== 200) return;

    console.log('评论数据:', data.data);

    const currentUserId = getUserIdFromToken();
    
    let postAuthorId = null;
    try {
        const postRes = await fetchWithAuth(`/post/detail/${pid}`);
        const postData = await postRes.json();
        if (postData.code === 200) {
            postAuthorId = postData.data.userId;
        }
    } catch (e) {
        console.error('获取帖子信息失败:', e);
    }

    let html = "";
    const maxVisible = 8;
    const totalComments = data.data.length;
    
    data.data.forEach((c, index) => {
        console.log('单条评论:', c);
        console.log('createTime:', c.createTime);
        
        let timeStr = '';
        if (c.createTime) {
            timeStr = formatCommentTime(c.createTime);
        }
        
        let deleteBtn = '';
        if (currentUserId && (c.userId === currentUserId || postAuthorId === currentUserId)) {
            deleteBtn = `<span style="color: #ff4d4f; cursor: pointer; margin-left: 10px; font-size: 12px;" onclick="event.stopPropagation(); deleteComment(${c.id}, ${pid})">删除</span>`;
        }
        
        // 超过 8 条评论，给第 9 条及以后的添加 hidden 样式
        const hiddenStyle = index >= maxVisible ? 'style="display:none;"' : '';
        
        html += `
        <div class="comment-item" ${hiddenStyle} data-index="${index}">
            <div class="comment-header">
                <div class="comment-author">${c.username}</div>
                <div style="display: flex; align-items: center;">
                    <div class="comment-time">${timeStr}</div>
                    ${deleteBtn}
                </div>
            </div>
            <div class="comment-text">${c.content}</div>
        </div>`;
    });
    
    const cmtEl = document.getElementById("cmt" + pid);
    if (cmtEl) {
        const needExpand = totalComments > maxVisible;
        
        cmtEl.innerHTML = `
            <div id="commentContainer-${pid}">
                ${html}
            </div>
            ${needExpand ? `<button class="expand-btn show" id="expandBtn-${pid}" onclick="toggleExpand(${pid}, ${totalComments})" style="display:block; width:100%; padding:10px; background:#f5f7fb; border:none; border-radius:6px; cursor:pointer; color:#1890ff; font-size:14px; margin-top:10px;">展开 ${totalComments - maxVisible} 条隐藏评论</button>` : ''}
        `;
    }
}

// 展开/折叠评论
function toggleExpand(pid, totalComments) {
    const container = document.getElementById(`commentContainer-${pid}`);
    const expandBtn = document.getElementById(`expandBtn-${pid}`);
    
    if (!container || !expandBtn) return;
    
    const hiddenItems = container.querySelectorAll('.comment-item[data-index]');
    let isExpanded = false;
    
    // 检查当前状态
    hiddenItems.forEach(item => {
        const index = parseInt(item.getAttribute('data-index'));
        if (index >= 8 && item.style.display !== 'none') {
            isExpanded = true;
        }
    });
    
    if (isExpanded) {
        // 折叠：隐藏第 9 条及以后
        hiddenItems.forEach(item => {
            const index = parseInt(item.getAttribute('data-index'));
            if (index >= 8) {
                item.style.display = 'none';
            }
        });
        expandBtn.textContent = `展开 ${totalComments - 8} 条隐藏评论`;
    } else {
        // 展开：显示所有
        hiddenItems.forEach(item => {
            item.style.display = 'block';
        });
        expandBtn.textContent = '收起回复';
    }
}

// 删除评论
async function deleteComment(commentId, postId) {
    if (!confirm('确定要删除这条评论吗？')) {
        return;
    }
    
    try {
        const res = await fetchWithAuth(`/comment/delete/${commentId}`, {
            method: 'DELETE'
        });
        
        const data = await res.json();
        if (data.code === 200) {
            // 删除成功，重新加载评论列表
            loadComments(postId);
        } else {
            alert(data.msg || '删除失败');
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('删除出错，请重试');
    }
}

// 从 token 获取用户 ID
function getUserIdFromToken() {
    const token = getToken();
    if (!token) return null;
    
    try {
        const payload = token.split('.')[1];
        const decoded = JSON.parse(atob(payload));
        return decoded.id || decoded.userId;
    } catch (e) {
        console.error('解析 token 失败:', e);
        return null;
    }
}

// 格式化评论时间
function formatCommentTime(timeStr) {
    if (!timeStr) return '';
    
    // 如果是数组格式 [2024,1,15,10,30,0]
    if (Array.isArray(timeStr)) {
        const [year, month, day, hour, minute, second] = timeStr;
        const date = new Date(year, month - 1, day, hour, minute, second);
        return formatRelativeTime(date);
    }
    
    // 如果是字符串格式 "2024-01-15T10:30:00"
    try {
        const date = new Date(timeStr);
        return formatRelativeTime(date);
    } catch (e) {
        return timeStr;
    }
}

// 格式化相对时间
function formatRelativeTime(date) {
    const now = new Date();
    const diff = now - date;
    
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    const months = Math.floor(days / 30);
    const years = Math.floor(months / 12);
    
    if (years > 0) {
        return `${years}年前`;
    } else if (months > 0) {
        return `${months}个月前`;
    } else if (days > 0) {
        return `${days}天前`;
    } else if (hours > 0) {
        return `${hours}小时前`;
    } else if (minutes > 0) {
        return `${minutes}分钟前`;
    } else if (seconds > 0) {
        return `${seconds}秒前`;
    } else {
        return '刚刚';
    }
}

// 6. 加载帖子列表（分页）
const pageSize = 10;

async function loadList(page = 1) {
    showLoading("list");
    
    const res = await fetchWithAuth(`/post/list?page=${page}&size=${pageSize}`);
    const data = await res.json();

    if (data.code !== 200) {
        alert("帖子加载失败");
        return;
    }

    const result = data.data;
    if (result.list) {
        renderPostList(result.list);
    }

    const paginationEl = document.getElementById("pagination");
    if (paginationEl) {
        let paginationHtml = '';
        if (result.page > 1) {
            paginationHtml += `<button onclick="loadList(${result.page - 1})">上一页</button>`;
        }
        paginationHtml += `<span>第 ${result.page} / ${result.pages} 页，共 ${result.total} 条</span>`;
        if (result.page < result.pages) {
            paginationHtml += `<button onclick="loadList(${result.page + 1})">下一页</button>`;
        }
        paginationEl.innerHTML = paginationHtml;
        paginationEl.style.display = "flex";
    }
}

// 7. 检查登录状态（通用）
function checkLogin() {
    if (!isLoggedIn()) {
        alert("请先登录！");
        location.href = "login.html";
        return false;
    }
    return true;
}
