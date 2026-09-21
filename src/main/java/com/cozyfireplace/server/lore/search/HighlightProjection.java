package com.cozyfireplace.server.lore.search;

/**
 * Строка пакетной подсветки: ord — порядковый номер сегмента в плоском списке
 * сегментов всей страницы, headline — результат ts_headline для него.
 */
public interface HighlightProjection {

    Long getOrd();

    String getHeadline();
}

