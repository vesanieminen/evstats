// Pull in vaadin-charts first so the Highcharts core and exporting module are
// already registered when offline-exporting tries to compose itself onto Chart.
// Without this, the bundler can interleave the modules and offline-exporting
// runs before Highcharts.Chart exists, throwing
// "Cannot read properties of undefined (reading 'prototype')" at compose time.
import '@vaadin/charts';
import 'highcharts/es-modules/masters/modules/offline-exporting.src.js';
