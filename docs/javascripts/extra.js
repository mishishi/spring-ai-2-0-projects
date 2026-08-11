// 标记首页 — mkdocs 默认 body 没特殊 class, 用 JS 加
(function() {
  if (window.location.pathname === '/' || window.location.pathname.endsWith('/index.html')) {
    document.body.classList.add('md-home');
  }
})();

// 面包屑 + 上一章/下一章 导航(适配 v2: docs 根目录结构, /NN-xxx/)
(function() {
  if (document.body.classList.contains('md-home')) return;

  // 从 URL 找 phase + chapter(v2: 根目录 /12-rag-production/)
  const path = window.location.pathname;
  const match = path.match(/\/(\d{2})-([^/]+)\//);
  if (!match) return;
  const chapterNum = parseInt(match[1], 10);
  const chapterSlug = match[2];

  // Phase 划分: 1-6 是 phase 1, 7-12 是 phase 2
  const phaseNum = chapterNum <= 6 ? 1 : 2;
  const phaseNames = {
    '1': 'Phase 1 · 基础',
    '2': 'Phase 2 · RAG',
  };
  const phaseName = phaseNames[phaseNum] || `Phase ${phaseNum}`;

  // 从 primary sidebar 找当前 active link + prev/next
  const primaryNav = document.querySelector('.md-nav.md-nav--primary');
  if (!primaryNav) return;
  const allLinks = Array.from(primaryNav.querySelectorAll('a.md-nav__link'));
  const currentIndex = allLinks.findIndex(a => {
    const href = a.getAttribute('href');
    return href && href.includes('/' + chapterNum + '-');
  });
  if (currentIndex === -1) return;

  const currentLink = allLinks[currentIndex];
  const prevLink = currentIndex > 0 ? allLinks[currentIndex - 1] : null;
  const nextLink = currentIndex < allLinks.length - 1 ? allLinks[currentIndex + 1] : null;

  const currentTitle = (currentLink.textContent || '').trim();

  // 找 H1 插 breadcrumb
  const h1 = document.querySelector('.md-content__inner h1');
  if (h1) {
    const breadcrumb = document.createElement('nav');
    breadcrumb.className = 'breadcrumb';
    breadcrumb.setAttribute('aria-label', '面包屑');
    let html = '<a href="/">首页</a>';
    html += '<span class="breadcrumb__sep">›</span>';
    html += `<a href="/overviews/phase-${phaseNum}/">${phaseName}</a>`;
    html += '<span class="breadcrumb__sep">›</span>';
    html += `<span class="breadcrumb__current">${currentTitle}</span>`;
    breadcrumb.innerHTML = html;
    h1.parentNode.insertBefore(breadcrumb, h1);
  }

  // 找正文末尾插 prev/next
  const content = document.querySelector('.md-content__inner');
  if (!content) return;
  const nav = document.createElement('nav');
  nav.className = 'chapter-nav';
  nav.setAttribute('aria-label', '章节导航');

  if (prevLink) {
    const href = prevLink.getAttribute('href');
    const title = (prevLink.textContent || '').trim();
    nav.innerHTML += `<a class="chapter-nav__item chapter-nav__prev" href="${href}">
      <span class="chapter-nav__label"><span class="chapter-nav__arrow">←</span> 上一章</span>
      <span class="chapter-nav__title">${title}</span>
    </a>`;
  } else {
    nav.innerHTML += `<span class="chapter-nav__item chapter-nav__item--empty"></span>`;
  }

  if (nextLink) {
    const href = nextLink.getAttribute('href');
    const title = (nextLink.textContent || '').trim();
    nav.innerHTML += `<a class="chapter-nav__item chapter-nav__next" href="${href}">
      <span class="chapter-nav__label">下一章 <span class="chapter-nav__arrow">→</span></span>
      <span class="chapter-nav__title">${title}</span>
    </a>`;
  } else {
    nav.innerHTML += `<span class="chapter-nav__item chapter-nav__item--empty"></span>`;
  }

  content.appendChild(nav);
})();

// 抽屉式目录 — 兼容 v1 风格(隐藏真 sidebar,但提供 toggle 入口)
(function() {
  if (!document.body.classList.contains('md-home')) return;
  if (document.querySelector('.nav-drawer')) return;

  // Trigger 按钮
  const trigger = document.createElement('button');
  trigger.className = 'nav-drawer-trigger';
  trigger.textContent = '目录';
  trigger.setAttribute('aria-label', '打开目录');
  document.body.appendChild(trigger);

  // Overlay
  const overlay = document.createElement('div');
  overlay.className = 'nav-drawer-overlay';
  document.body.appendChild(overlay);

  // Drawer
  const drawer = document.createElement('aside');
  drawer.className = 'nav-drawer';
  drawer.setAttribute('aria-label', '目录');

  // Close 按钮
  const close = document.createElement('button');
  close.className = 'nav-drawer__close';
  close.textContent = '×';
  close.setAttribute('aria-label', '关闭目录');
  drawer.appendChild(close);

  // 标题
  const title = document.createElement('h2');
  title.className = 'nav-drawer__title';
  title.textContent = 'Spring AI 2.0 项目实战';
  drawer.appendChild(title);

  const sub = document.createElement('span');
  sub.className = 'nav-drawer__sub';
  sub.textContent = '12 章 · 5 个项目 · 20 周';
  drawer.appendChild(sub);

  // 从 mkdocs 默认 sidebar 复制 nav 结构
  const primaryNav = document.querySelector('.md-nav.md-nav--primary');
  if (primaryNav) {
    // 复制所有顶级链接
    const topLevel = primaryNav.querySelectorAll(':scope > .md-nav__list > .md-nav__item');
    topLevel.forEach(item => {
      const link = item.querySelector(':scope > a.md-nav__link, :scope > .md-nav__link');
      if (link) {
        const text = (link.textContent || '').trim();
        // 顶级链接(Phase 标题)可能没 href(label only),不显示
        if (link.getAttribute('href') || text.includes('Phase')) {
          // 跳过 phase 标题本身,只显示其下的 chapter
          if (!text.includes('Phase')) {
            const a = document.createElement('a');
            a.href = link.getAttribute('href') || '#';
            a.textContent = text;
            a.style.fontWeight = '700';
            drawer.appendChild(a);
          } else {
            // Phase 分组标题
            const sub = document.createElement('span');
            sub.className = 'nav-drawer__sub';
            sub.textContent = text;
            drawer.appendChild(sub);
          }
        }

        // 只加 chapter 级别链接(顶级 .md-nav__list > .md-nav__item > a.md-nav__link),不加 H2
        const subItems = item.querySelectorAll(':scope > .md-nav > .md-nav__list > .md-nav__item > a.md-nav__link');
        subItems.forEach(subLink => {
          const a2 = document.createElement('a');
          a2.href = subLink.getAttribute('href') || '#';
          const subText = (subLink.textContent || '').trim();
          // 拆分数字和标题,如 "01 Hello World" → strong "01" + "Hello World"
          const match = subText.match(/^(\d+)\s+(.+)$/);
          if (match) {
            const num = document.createElement('span');
            num.className = 'nav-drawer__num';
            num.textContent = match[1];
            a2.appendChild(num);
            a2.appendChild(document.createTextNode(' ' + match[2]));
          } else {
            a2.textContent = subText;
          }
          a2.style.paddingLeft = '1.2rem';
          a2.style.fontSize = '0.88rem';
          drawer.appendChild(a2);
        });
      }
    });
  }
  document.body.appendChild(drawer);

  // Toggle 逻辑
  function open() {
    drawer.classList.add('is-open');
    overlay.classList.add('is-open');
  }
  function closeFn() {
    drawer.classList.remove('is-open');
    overlay.classList.remove('is-open');
  }
  trigger.addEventListener('click', open);
  close.addEventListener('click', closeFn);
  overlay.addEventListener('click', closeFn);
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && drawer.classList.contains('is-open')) closeFn();
  });
})();
