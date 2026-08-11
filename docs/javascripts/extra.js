// 标记首页 — mkdocs-material 9.5 移除了 body.md-home class,用 JS 加
// 同时所有页面都加(magazine 装饰 header 全站显示),不光是首页
// 关键: Vercel 部署会 rewrite /01-hello-world/ 而非 /01-hello-world/index.html,
// 所以 endsWith('/index.html') 在线上不匹配,改用更宽松的判断
(function() {
  const p = window.location.pathname;
  if (p === '/' || p.endsWith('/index.html') || /\/[^/]+\/?$/.test(p)) {
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

// 抽屉式目录 — 兼容 v1 风格(隐藏真 sidebar,左下角按钮唤起,12 章直接列出)
(function() {
  if (!document.body.classList.contains('md-home')) return;
  if (document.querySelector('.nav-drawer')) return;

  // Trigger 按钮
  const trigger = document.createElement('button');
  trigger.className = 'nav-drawer-trigger';
  trigger.setAttribute('aria-label', '打开目录');
  trigger.setAttribute('title', '目录');
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
  title.textContent = '目录 · 23 module';
  drawer.appendChild(title);

  // 23 module 全部列出(从 mkdocs nav 提取 chapter 级别链接,跳过 "Phase X 总览" 标签)
  const primaryNav = document.querySelector('.md-nav.md-nav--primary');
  if (primaryNav) {
    const allChapterLinks = primaryNav.querySelectorAll(
      ':scope > .md-nav__list > .md-nav__item > .md-nav > .md-nav__list > .md-nav__item > a.md-nav__link'
    );
    allChapterLinks.forEach(subLink => {
      const subText = (subLink.textContent || '').trim();
      // 只显示 01-18 chapter + P1-P5 项目,跳过 "Phase X 总览"
      if (!/^(0?\d+|P[1-5])\s+/.test(subText)) return;

      const a = document.createElement('a');
      a.href = subLink.getAttribute('href') || '#';
      const match = subText.match(/^(0?\d+|P[1-5])\s+(.+)$/);
      if (match) {
        const num = document.createElement('span');
        num.className = 'nav-drawer__num';
        num.textContent = match[1];
        a.appendChild(num);
        a.appendChild(document.createTextNode(' ' + match[2]));
      } else {
        a.textContent = subText;
      }
      drawer.appendChild(a);
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
