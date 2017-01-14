module.exports = function (config) {
    config.set({
        files: [
            'lib/angular/angular.js',
            'lib/angular-mocks/angular-mocks.js',
            'build/ng-lodash.js',
            'test/ng-lodash.spec.js'
        ],
        exclude: [],
        browsers: ['PhantomJS'],
        frameworks: ['jasmine'],
        port: 9876,
        runnerPort: 9100,
        autoWatch: true,
        captureTimeout: 60000,
        singleRun: true
    });
};
