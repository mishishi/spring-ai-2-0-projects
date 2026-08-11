// 标记所有页面为 md-home(全站杂志风装饰 header)
// Vercel 部署会 rewrite /01-hello-world/ 而非 /01-hello-world/index.html,
// 所以 endsWith('/index.html') 在线上不匹配,改用更宽松的判断
(function() {
  const p = window.location.pathname;
  if (p === '/' || p.endsWith('/index.html') || /\/[^/]+\/?$/.test(p)) {
    document.body.classList.add('md-home');
  }
  // 真首页另外加 md-home-hero(用于 chapter-nav 不显示等特例)
  if (p === '/' || p === '/index.html' || p.endsWith('/index.html') && document.querySelector('.md-content__inner > h1')?.textContent === 'Spring AI 2.0 项目实战') {
    document.body.classList.add('md-home-hero');
  }
})();

// 面包屑 + 上一章/下一章 导航
// 适配 v2: 根目录结构,支持 01-18 chapter + P1-P5 项目
(function() {
  // 真首页不显示
  if (document.body.classList.contains('md-home-hero')) return;

  const path = window.location.pathname;
  // 扩展 regex: 支持 NN-xxx (01-18) 或 project-N-xxx (P1-P5)
  const match = path.match(/\/(?:(\d{2})|project-([1-5]))-([^/]+)\//);
  if (!match) return;

  const numStr = match[1] || ('P' + match[2]);
  // Phase 划分: 1-6=phase 1, 7-12=phase 2, 13-18=phase 3, P1-P5=phase 4
  let phaseNum;
  if (numStr.startsWith('P')) {
    phaseNum = 4;
  } else {
    const num = parseInt(numStr, 10);
    if (num <= 6) phaseNum = 1;
    else if (num <= 12) phaseNum = 2;
    else phaseNum = 3;
  }
  const phaseNames = {
    '1': 'Phase 1 · 基础',
    '2': 'Phase 2 · RAG',
    '3': 'Phase 3 · Agent',
    '4': 'Phase 4 · 完整项目',
  };
  const phaseName = phaseNames[phaseNum];

  const primaryNav = document.querySelector('.md-nav.md-nav--primary');
  if (!primaryNav) return;
  const allLinks = Array.from(primaryNav.querySelectorAll('a.md-nav__link'));
  // 找当前 active link — mkdocs 给当前页加 md-nav__link--active
  // 不用 href 匹配是因为 Vercel 部署后 href 是相对路径(甚至 "./"),不靠谱
  let currentIndex = allLinks.findIndex(a => a.classList.contains('md-nav__link--active'));
  // fallback: 找 text 匹配 numStr 的 link
  if (currentIndex === -1) {
    currentIndex = allLinks.findIndex(a => {
      const t = (a.textContent || '').trim();
      return t.startsWith(numStr + ' ') || t.startsWith(numStr + '·') || t === numStr;
    });
  }
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

// 主题切换按钮(右上 ⌘K 搜索 旁)
(function() {
  if (document.querySelector('.theme-toggle')) return;
  const btn = document.createElement('button');
  btn.className = 'theme-toggle';
  btn.setAttribute('aria-label', '切换深色模式');
  btn.setAttribute('title', '切换深色模式');
  btn.innerHTML = '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z"/></svg>';
  document.body.appendChild(btn);

  // 初始化:从 localStorage 读,或跟随系统
  const stored = localStorage.getItem('md-color-scheme');
  if (stored === 'slate' || stored === 'default') {
    document.body.setAttribute('data-md-color-scheme', stored);
  }
  updateIcon();

  btn.addEventListener('click', () => {
    const cur = document.body.getAttribute('data-md-color-scheme') ||
                (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'slate' : 'default');
    const next = cur === 'slate' ? 'default' : 'slate';
    document.body.setAttribute('data-md-color-scheme', next);
    localStorage.setItem('md-color-scheme', next);
    updateIcon();
  });

  function updateIcon() {
    const cur = document.body.getAttribute('data-md-color-scheme') ||
                (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'slate' : 'default');
    btn.innerHTML = cur === 'slate'
      ? '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0-5l-1.5 1.5L12 5l1.5-1.5L12 2zm0 20l-1.5-1.5L12 19l1.5 1.5L12 22zM4 12l-1.5-1.5L1 12l1.5 1.5L4 12zm17 0l1.5-1.5L24 12l-1.5 1.5L21 12zM5.6 5.6L4.1 4.1 2.6 5.6 4.1 7.1 5.6 5.6zm12.8 12.8l1.5 1.5 1.5-1.5-1.5-1.5-1.5 1.5zM5.6 18.4l-1.5 1.5 1.5 1.5 1.5-1.5-1.5-1.5zm12.8-12.8l-1.5-1.5 1.5-1.5 1.5 1.5-1.5 1.5z"/></svg>'
      : '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z"/></svg>';
  }
})();

// 返回顶部按钮(滚动后右下角出现)
(function() {
  if (document.querySelector('.back-to-top')) return;
  const btn = document.createElement('button');
  btn.className = 'back-to-top';
  btn.setAttribute('aria-label', '返回顶部');
  btn.setAttribute('title', '返回顶部');
  btn.innerHTML = '↑';
  document.body.appendChild(btn);

  function check() {
    if (window.scrollY > 400) {
      btn.classList.add('is-visible');
    } else {
      btn.classList.remove('is-visible');
    }
  }
  window.addEventListener('scroll', check, { passive: true });
  check();

  btn.addEventListener('click', () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });
})();

// 抽屉式目录 + Phase 分组 + 搜索过滤
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

  // 搜索框
  const searchWrap = document.createElement('div');
  searchWrap.className = 'nav-drawer__search';
  const search = document.createElement('input');
  search.type = 'text';
  search.placeholder = '🔍 过滤章节(支持中文)...';
  search.className = 'nav-drawer__search-input';
  searchWrap.appendChild(search);
  drawer.appendChild(searchWrap);

  // 23 module 按 Phase 分组
  const container = document.createElement('div');
  container.className = 'nav-drawer__groups';

  const phaseGroups = {
    'Phase 1 · 基础': [],
    'Phase 2 · RAG': [],
    'Phase 3 · Agent': [],
    'Phase 4 · 完整项目': [],
  };

  const primaryNav = document.querySelector('.md-nav.md-nav--primary');
  if (primaryNav) {
    const allChapterLinks = primaryNav.querySelectorAll(
      ':scope > .md-nav__list > .md-nav__item > .md-nav > .md-nav__list > .md-nav__item > a.md-nav__link'
    );
    allChapterLinks.forEach(subLink => {
      const subText = (subLink.textContent || '').trim();
      if (!/^(0?\d+|P[1-5])\s+/.test(subText)) return;
      // 过滤首页 H2 锚点(用 href 包含 # 的)和非 chapter 页
      const href = subLink.getAttribute('href') || '';
      if (href.startsWith('#')) return;  // 锚点
      if (href.includes('overviews')) return;  // 总览页
      if (href.includes('00-reading')) return;  // 导读
      if (href.includes('retrospectives')) return;  // 复盘
      if (href.includes('decisions')) return;  // 决策记录
      if (href.endsWith('index') || href.endsWith('index/')) return;  // 首页
      const match = subText.match(/^(0?\d+|P[1-5])\s+(.+)$/);
      const num = match[1];
      const title = match[2];
      let phase;
      if (num.startsWith('P')) phase = 'Phase 4 · 完整项目';
      else {
        const n = parseInt(num, 10);
        if (n <= 6) phase = 'Phase 1 · 基础';
        else if (n <= 12) phase = 'Phase 2 · RAG';
        else phase = 'Phase 3 · Agent';
      }
      phaseGroups[phase].push({ num, title, href });
    });
  }

  // 当前页面所属 phase:默认全部展开
  const currentPath = window.location.pathname;
  const currentMatch = currentPath.match(/\/(\d{2}|P[1-5])-/);
  let currentPhase = null;
  if (currentMatch) {
    if (currentMatch[1].startsWith('P')) currentPhase = 'Phase 4 · 完整项目';
    else {
      const n = parseInt(currentMatch[1], 10);
      if (n <= 6) currentPhase = 'Phase 1 · 基础';
      else if (n <= 12) currentPhase = 'Phase 2 · RAG';
      else currentPhase = 'Phase 3 · Agent';
    }
  }

  Object.entries(phaseGroups).forEach(([phaseName, items]) => {
    const group = document.createElement('div');
    group.className = 'nav-drawer__group';
    if (currentPhase && phaseName !== currentPhase) {
      group.classList.add('is-collapsed');
    }

    const header = document.createElement('div');
    header.className = 'nav-drawer__group-header';
    header.innerHTML = `<span class="nav-drawer__group-toggle">▼</span><span class="nav-drawer__group-name">${phaseName}</span><span class="nav-drawer__group-count">${items.length}</span>`;
    header.addEventListener('click', () => group.classList.toggle('is-collapsed'));
    group.appendChild(header);

    const list = document.createElement('div');
    list.className = 'nav-drawer__group-list';
    items.forEach(item => {
      const a = document.createElement('a');
      a.href = item.href;
      a.dataset.searchText = (item.num + ' ' + item.title).toLowerCase();
      const numEl = document.createElement('span');
      numEl.className = 'nav-drawer__num';
      numEl.textContent = item.num;
      a.appendChild(numEl);
      a.appendChild(document.createTextNode(' ' + item.title));
      list.appendChild(a);
    });
    group.appendChild(list);
    container.appendChild(group);
  });

  drawer.appendChild(container);
  document.body.appendChild(drawer);

  // 搜索过滤
  search.addEventListener('input', () => {
    const q = search.value.trim().toLowerCase();
    const groups = container.querySelectorAll('.nav-drawer__group');
    groups.forEach(g => {
      const links = g.querySelectorAll('a');
      let visibleCount = 0;
      links.forEach(a => {
        const text = a.dataset.searchText;
        if (!q || text.includes(q) || a.textContent.toLowerCase().includes(q)) {
          a.style.display = '';
          visibleCount++;
        } else {
          a.style.display = 'none';
        }
      });
      g.querySelector('.nav-drawer__group-count').textContent = visibleCount;
      if (q && visibleCount === 0) {
        g.style.display = 'none';
      } else {
        g.style.display = '';
        if (q) g.classList.remove('is-collapsed');
      }
    });
  });

  // Toggle 逻辑
  function open() {
    drawer.classList.add('is-open');
    overlay.classList.add('is-open');
    setTimeout(() => search.focus(), 300);
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
    // ⌘K / Ctrl+K 也开 drawer
    if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
      e.preventDefault();
      drawer.classList.contains('is-open') ? closeFn() : open();
    }
  });
})();
