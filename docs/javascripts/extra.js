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
