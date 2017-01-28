// Karma configuration
// http://karma-runner.github.io/0.13/config/configuration-file.html

var sourcePreprocessors = ['coverage'];

function isDebug() {
    return process.argv.indexOf('--debug') >= 0;
}

if (isDebug()) {
    // Disable JS minification if Karma is run with debug option.
    sourcePreprocessors = [];
}

module.exports = function (config) {
    config.set({
        // base path, that will be used to resolve files and exclude
        basePath: 'src/test/javascript/'.replace(/[^/]+/g, '..'),

        // testing framework to use (jasmine/mocha/qunit/...)
        frameworks: ['jasmine'],

        // list of files / patterns to load in the browser
        files: [
            // bower:js
            'src/main/webapp/bower_components/jquery/dist/jquery.js',
            'src/main/webapp/bower_components/json3/lib/json3.js',
            'src/main/webapp/bower_components/messageformat/messageformat.js',
            'src/main/webapp/bower_components/lodash/lodash.js',
            'src/main/webapp/bower_components/sockjs-client/dist/sockjs.js',
            'src/main/webapp/bower_components/stomp-websocket/lib/stomp.min.js',
            'src/main/webapp/bower_components/bootstrap-datepicker/dist/js/bootstrap-datepicker.min.js',
            'src/main/webapp/bower_components/bootstrap-select/dist/js/bootstrap-select.js',
            'src/main/webapp/bower_components/bootstrap-switch/dist/js/bootstrap-switch.js',
            'src/main/webapp/bower_components/bootstrap-touchspin/src/jquery.bootstrap-touchspin.js',
            'src/main/webapp/bower_components/d3/d3.js',
            'src/main/webapp/bower_components/datatables/media/js/jquery.dataTables.js',
            'src/main/webapp/bower_components/datatables-colreorder/js/dataTables.colReorder.js',
            'src/main/webapp/bower_components/datatables-colvis/js/dataTables.colVis.js',
            'src/main/webapp/bower_components/google-code-prettify/bin/prettify.min.js',
            'src/main/webapp/bower_components/matchHeight/dist/jquery.matchHeight.js',
            'src/main/webapp/bower_components/moment/moment.js',
            'src/main/webapp/bower_components/patternfly-bootstrap-combobox/js/bootstrap-combobox.js',
            'src/main/webapp/bower_components/patternfly-bootstrap-treeview/dist/bootstrap-treeview.min.js',
            'src/main/webapp/bower_components/nvd3/build/nv.d3.js',
            'src/main/webapp/bower_components/angular/angular.js',
            'src/main/webapp/bower_components/angular-aria/angular-aria.js',
            'src/main/webapp/bower_components/angular-bootstrap/ui-bootstrap-tpls.js',
            'src/main/webapp/bower_components/angular-cache-buster/angular-cache-buster.js',
            'src/main/webapp/bower_components/angular-cookies/angular-cookies.js',
            'src/main/webapp/bower_components/ngstorage/ngStorage.js',
            'src/main/webapp/bower_components/angular-loading-bar/build/loading-bar.js',
            'src/main/webapp/bower_components/angular-resource/angular-resource.js',
            'src/main/webapp/bower_components/angular-sanitize/angular-sanitize.js',
            'src/main/webapp/bower_components/angular-ui-router/release/angular-ui-router.js',
            'src/main/webapp/bower_components/bootstrap-ui-datetime-picker/dist/datetime-picker.js',
            'src/main/webapp/bower_components/ng-file-upload/ng-file-upload.js',
            'src/main/webapp/bower_components/ngInfiniteScroll/build/ng-infinite-scroll.js',
            'src/main/webapp/bower_components/angular-relative-date/dist/angular-relative-date.js',
            'src/main/webapp/bower_components/c3/c3.js',
            'src/main/webapp/bower_components/eonasdan-bootstrap-datetimepicker/build/js/bootstrap-datetimepicker.min.js',
            'src/main/webapp/bower_components/ng-sortable/dist/ng-sortable.js',
            'src/main/webapp/bower_components/angular-animate/angular-animate.js',
            'src/main/webapp/bower_components/angular-nvd3/dist/angular-nvd3.js',
            'src/main/webapp/bower_components/angular-mocks/angular-mocks.js',
            'src/main/webapp/bower_components/patternfly/dist/js/patternfly.js',
            'src/main/webapp/bower_components/angular-patternfly/dist/angular-patternfly.js',
            'src/main/webapp/bower_components/angular-key-value-editor/dist/angular-key-value-editor.js',
            'src/main/webapp/bower_components/angular-key-value-editor/dist/compiled-templates.js',
            // endbower
            'src/main/webapp/app/app.module.js',
            'src/main/webapp/app/app.state.js',
            'src/main/webapp/app/app.constants.js',
            'src/main/webapp/app/**/*.+(js|html)',
            'src/test/javascript/spec/helpers/module.js',
            'src/test/javascript/spec/helpers/httpBackend.js',
            'src/test/javascript/**/!(karma.conf).js'
        ],


        // list of files / patterns to exclude
        exclude: [],

        preprocessors: {
            './**/*.js': sourcePreprocessors
        },

        reporters: ['dots', 'junit', 'coverage', 'progress'],

        junitReporter: {
            outputFile: '../build/test-results/karma/TESTS-results.xml'
        },

        coverageReporter: {
            dir: 'build/test-results/coverage',
            reporters: [
                {type: 'lcov', subdir: 'report-lcov'}
            ]
        },

        // web server port
        port: 9876,

        // level of logging
        // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
        logLevel: config.LOG_INFO,

        // enable / disable watching file and executing tests whenever any file changes
        autoWatch: false,

        // Start these browsers, currently available:
        // - Chrome
        // - ChromeCanary
        // - Firefox
        // - Opera
        // - Safari (only Mac)
        // - PhantomJS
        // - IE (only Windows)
        browsers: ['PhantomJS'],

        // Continuous Integration mode
        // if true, it capture browsers, run tests and exit
        singleRun: false,

        // to avoid DISCONNECTED messages when connecting to slow virtual machines
        browserDisconnectTimeout: 10000, // default 2000
        browserDisconnectTolerance: 1, // default 0
        browserNoActivityTimeout: 4 * 60 * 1000 //default 10000
    });
};
