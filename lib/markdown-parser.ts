// utils/markdown.ts
import MarkdownIt from 'markdown-it';
import container from 'markdown-it-container';
import anchor from 'markdown-it-anchor';
import slugify from '@sindresorhus/slugify';


// Уберите лишние пробелы в конце!
const BASE_URL = (process.env.NEXT_PUBLIC_BASE_URL || 'https://example.com').trim();
const normalizeUrl = (url: string) => {
  try {
    const urlObj = new URL(url);
    // Убираем www. если есть
    urlObj.hostname = urlObj.hostname.replace(/^www\./, '');
    return urlObj.toString();
  } catch (e) {
    return url; // Если невалидный URL, возвращаем как есть
  }
};
const BASE_URL_NORMALIZED = normalizeUrl(BASE_URL).replace(/\/+$/, '');
function escapeRegExp(str: string): string {
  return str.replaceAll(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export interface MarkdownParserOptions {
  anchorClickable?: boolean;
  showExcerpts?: boolean;
  enableContainer?: boolean;
}

export function createMarkdownParser(options?: MarkdownParserOptions) {
  const { anchorClickable = false, showExcerpts = false, enableContainer = true } = options || {};

  const md = new MarkdownIt({
    html: true,
    linkify: false,
    typographer: true,
  });

  // Включаем emphasis для курсива через _так_ (без требования пробелов)
  md.enable('emphasis');
  

  if (anchorClickable) {
    // Add anchor plugin to generate IDs for headings
    md.use(anchor, {
      permalink: anchor.permalink.linkAfterHeader({
        style: 'aria-label',
        assistiveText: (title) => `Permalink to "${title}"`,
        symbol: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="heading-link-icon-svg"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg>`,
        renderHref: (slug, opts) => `#${slug}`,
        wrapper: ['<span class="header-anchor-wrapper">', '</span>'],
      }),
      slugify: (s: string) => slugify(s, { separator: '-', preserveTrailingDash: false, transliterate: false }),
    });
  } else if (!anchorClickable) {
    // Use linkInsideHeader functionality when anchorClickable is false
    md.use(anchor, {
      slugify: (s: string) => slugify(s, { separator: '-', preserveTrailingDash: false, transliterate: false }),
    });
  }

  if (enableContainer) {
    md.use(container as any, 'restrictedBlock', {
      render: (tokens: any[], idx: number) => {
        if (tokens[idx].nesting === 1) {
          return '<div class="restricted-block-container">\n';
        } else {
          return '</div>\n';
        }
      },
    });
  }

  if (showExcerpts) {
    const defaultRender = md.renderer.rules.link_open || ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options));

    md.renderer.rules.link_open = (tokens, idx, options, _env, self) => {
      const token = tokens[idx];
      token.attrs ??= [];

      const hrefIndex = token.attrIndex('href');
      if (hrefIndex >= 0) {
        const href = token.attrs[hrefIndex][1];
        const normalizedHref = normalizeUrl(href);
      
        if (normalizedHref.startsWith(BASE_URL_NORMALIZED)) {
          // Добавляем класс к существующим или создаем новый
          const classIndex = token.attrIndex('class');
          if (classIndex < 0) {
            token.attrPush(['class', 'internal-link']);
          } else {
            token.attrs[classIndex][1] += ' internal-link';
          }
        }

        // Строим паттерн один раз и используем напрямую
        const pattern = new RegExp(
          `^${escapeRegExp(BASE_URL_NORMALIZED)}/rooms/([^/]+)/lore/(\\d+)#(.+)$`
        );

        const match = pattern.exec(href);
        if (match) {
          const [, , loreId, encodedFragment] = match;
          const payload = `${loreId}#${encodedFragment}`;

          token.attrSet('data-lore-popup', payload);

          const classIndex = token.attrIndex('class');
          if (classIndex < 0) {
            token.attrPush(['class', 'lore-link']);
          } else {
            token.attrs[classIndex][1] += ' lore-link';
          }
        }
      }

      return defaultRender(tokens, idx, options, _env, self);
    };
  }

  return md;
}