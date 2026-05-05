package com.vesanieminen.components;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.Buttons;
import com.vaadin.flow.component.charts.model.ContextButton;
import com.vaadin.flow.component.charts.model.Exporting;
import com.vesanieminen.i18n.T;

/**
 * Adds a small in-chart export menu (⋮) with two options: Download PNG and
 * Download CSV. Implemented by:
 * <ul>
 *   <li>Enabling the Highcharts exporting module via Vaadin Charts'
 *       {@link Exporting} configuration so the context button is rendered.</li>
 *   <li>Injecting the actual menu items via JavaScript after the chart is
 *       initialised, because Vaadin Charts' Java {@code ContextButtonMenuItem}
 *       does not serialize the {@code onclick} function across to the
 *       browser, and the {@code export-data} module that provides
 *       {@code chart.downloadCSV()} is not loaded — so we build the CSV
 *       ourselves from the chart's series.</li>
 * </ul>
 * Filename is computed dynamically from the visible x-axis range.
 */
public final class ChartExport {

    private static final String INIT_JS = """
            const el = this;
            const slug = $0;
            const sanitize = (s) => String(s).replace(/[^a-zA-Z0-9_-]/g, '-');
            const buildFilename = (chart) => {
              const ax = chart.xAxis && chart.xAxis[0];
              if (!ax) return slug;
              const ext = ax.getExtremes();
              if (ax.options && ax.options.type === 'datetime' && isFinite(ext.min) && isFinite(ext.max)) {
                const s = new Date(ext.min).getUTCFullYear();
                const t = new Date(ext.max).getUTCFullYear();
                return s === t ? slug + '-' + s : slug + '-' + s + '-' + t;
              }
              if (ax.categories && ax.categories.length) {
                const i = Math.max(0, Math.floor(ext.min));
                const j = Math.min(ax.categories.length - 1, Math.ceil(ext.max));
                const a = sanitize(ax.categories[i]);
                const b = sanitize(ax.categories[j]);
                return a === b ? slug + '-' + a : slug + '-' + a + '-' + b;
              }
              return slug;
            };
            const csvCell = (v) => {
              if (v == null) return '';
              const s = String(v);
              if (/[",\\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"';
              return s;
            };
            const buildCsv = (chart) => {
              const ax = chart.xAxis && chart.xAxis[0];
              const ext = ax ? ax.getExtremes() : null;
              const isCategory = ax && ax.categories && ax.categories.length > 0;
              const isDatetime = ax && ax.options && ax.options.type === 'datetime';
              const xHeader = (ax && ax.options && ax.options.title && ax.options.title.text)
                || (isDatetime ? 'Date' : 'Category');
              const lines = [];
              const visibleSeries = chart.series.filter(s => s.visible !== false && s.options.showInLegend !== false);
              const header = [csvCell(xHeader)].concat(visibleSeries.map(s => csvCell(s.name)));
              lines.push(header.join(','));
              if (isCategory) {
                const cats = ax.categories;
                const i = ext ? Math.max(0, Math.floor(ext.min)) : 0;
                const j = ext ? Math.min(cats.length - 1, Math.ceil(ext.max)) : cats.length - 1;
                for (let k = i; k <= j; k++) {
                  const row = [csvCell(cats[k])];
                  for (const s of visibleSeries) {
                    const pt = s.points && s.points[k];
                    row.push(csvCell(pt && pt.y != null ? pt.y : ''));
                  }
                  lines.push(row.join(','));
                }
              } else {
                const xs = new Set();
                for (const s of visibleSeries) for (const p of s.points || []) xs.add(p.x);
                const sortedX = Array.from(xs).sort((a, b) => a - b)
                  .filter(x => !ext || (x >= ext.min && x <= ext.max));
                for (const x of sortedX) {
                  const xLabel = isDatetime ? new Date(x).toISOString().slice(0, 10) : String(x);
                  const row = [csvCell(xLabel)];
                  for (const s of visibleSeries) {
                    const pt = (s.points || []).find(p => p.x === x);
                    row.push(csvCell(pt && pt.y != null ? pt.y : ''));
                  }
                  lines.push(row.join(','));
                }
              }
              return lines.join('\\n');
            };
            const triggerDownload = (data, filename, mime) => {
              const blob = new Blob([data], { type: mime });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = filename;
              document.body.appendChild(a);
              a.click();
              document.body.removeChild(a);
              setTimeout(() => URL.revokeObjectURL(url), 0);
            };
            const apply = () => {
              const chart = el.configuration;
              if (!chart || typeof chart.update !== 'function') return false;
              const exportingOpts = {
                enabled: true,
                fallbackToExportServer: false,
                buttons: {
                  contextButton: {
                    symbol: 'menuball',
                    menuItems: [
                      { text: $1, onclick: function() {
                        // Custom PNG export. Charts run in styledMode, so the SVG only
                        // references CSS classes — Highcharts' offline-exporting rasteriser
                        // can't reproduce the on-screen colours in Safari (and the export
                        // server is blocked in production). We bake the live computed
                        // styles into the cloned SVG as inline style="..." attributes so
                        // the SVG renders identically in any browser's <img>/canvas
                        // pipeline, then convert to PNG ourselves.
                        const chart = this;
                        const filename = buildFilename(chart) + '.png';
                        const sourceSvg = chart.container.querySelector('svg');
                        if (!sourceSvg) return;
                        const cloneSvg = sourceSvg.cloneNode(true);
                        const props = ['fill','fill-opacity','stroke','stroke-opacity',
                          'stroke-width','stroke-dasharray','stroke-dashoffset',
                          'stroke-linecap','stroke-linejoin','opacity','visibility','color',
                          'font-family','font-size','font-weight','font-style',
                          'text-anchor','dominant-baseline','paint-order'];
                        const sourceEls = [sourceSvg, ...sourceSvg.querySelectorAll('*')];
                        const cloneEls = [cloneSvg, ...cloneSvg.querySelectorAll('*')];
                        for (let i = 0; i < sourceEls.length; i++) {
                          const cs = getComputedStyle(sourceEls[i]);
                          const decls = [];
                          for (const p of props) {
                            const v = cs.getPropertyValue(p);
                            if (v) decls.push(p + ':' + v);
                          }
                          if (decls.length) cloneEls[i].setAttribute('style', decls.join(';'));
                          cloneEls[i].removeAttribute('class');
                        }
                        cloneSvg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
                        const w = chart.chartWidth, h = chart.chartHeight;
                        const containerBg = getComputedStyle(chart.container).backgroundColor;
                        const bg = (containerBg && containerBg !== 'rgba(0, 0, 0, 0)' && containerBg !== 'transparent')
                          ? containerBg : '#ffffff';
                        const svgString = new XMLSerializer().serializeToString(cloneSvg);
                        const svgBlob = new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' });
                        const svgUrl = URL.createObjectURL(svgBlob);
                        const img = new Image();
                        img.onload = () => {
                          const scale = window.devicePixelRatio || 2;
                          const canvas = document.createElement('canvas');
                          canvas.width = Math.round(w * scale);
                          canvas.height = Math.round(h * scale);
                          const ctx = canvas.getContext('2d');
                          ctx.scale(scale, scale);
                          ctx.fillStyle = bg;
                          ctx.fillRect(0, 0, w, h);
                          ctx.drawImage(img, 0, 0, w, h);
                          URL.revokeObjectURL(svgUrl);
                          canvas.toBlob((pngBlob) => {
                            const dlUrl = URL.createObjectURL(pngBlob);
                            const a = document.createElement('a');
                            a.href = dlUrl;
                            a.download = filename;
                            document.body.appendChild(a);
                            a.click();
                            document.body.removeChild(a);
                            setTimeout(() => URL.revokeObjectURL(dlUrl), 0);
                          }, 'image/png');
                        };
                        img.onerror = () => URL.revokeObjectURL(svgUrl);
                        img.src = svgUrl;
                      } },
                      { text: $2, onclick: function() {
                        triggerDownload(buildCsv(this), buildFilename(this) + '.csv',
                          'text/csv;charset=utf-8;');
                      } }
                    ]
                  }
                }
              };
              // Prefer the targeted exporting.update API (rebuilds the menu DOM
              // without redrawing the whole chart). Fall back to chart.update
              // with redraw=true, which is needed because update(..., false)
              // leaves the menu DOM stale.
              if (chart.exporting && typeof chart.exporting.update === 'function') {
                chart.exporting.update(exportingOpts, true);
              } else {
                chart.update({ exporting: exportingOpts }, true);
              }
              el.setAttribute('data-export-ready', '');
              return true;
            };
            if (!apply()) {
              const start = Date.now();
              const timer = setInterval(() => {
                if (apply() || Date.now() - start > 10000) clearInterval(timer);
              }, 50);
            }
            """;

    private ChartExport() {
    }

    /**
     * Configures exporting on the given chart. Adds a 2-item menu (PNG / CSV)
     * with a filename based on the slug and the chart's visible x-axis range.
     *
     * @param chart        the chart to configure
     * @param filenameSlug filename prefix, e.g. "ev-registrations"
     */
    public static void configure(Chart chart, String filenameSlug) {
        final var safeSlug = filenameSlug.replaceAll("[^a-zA-Z0-9_-]", "-");

        // Server-side: enable the exporting module so the context button SVG
        // is rendered. The actual menu items are wired up client-side below.
        final var exporting = new Exporting(true);
        exporting.setFilename(safeSlug);
        exporting.setFallbackToExportServer(false);
        final var buttons = new Buttons();
        final var contextButton = new ContextButton();
        contextButton.setSymbol("menuball");
        buttons.setContextButton(contextButton);
        exporting.setButtons(buttons);
        chart.getConfiguration().setExporting(exporting);

        chart.getElement().executeJs(INIT_JS, safeSlug,
                T.tr("chart.download.png"), T.tr("chart.download.csv"));
    }
}
