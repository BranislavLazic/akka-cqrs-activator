module.exports = function (grunt) {
    grunt.initConfig({
        'bower': {
            'install': {
                'options': {
                    'targetDir': './lib'
                }
            }
        },
        'jshint': {
            'src': [
                './*.js',
                './test/*.js'
            ],
            'options': {
                'jshintrc': '.jshintrc'
            }
        },
        'jscs': {
            'src': '<%= jshint.src %>'
        },
        'karma': {
            'options': {
                'configFile': 'karma.conf.js'
            },
            'test': {
                'reporters': ['progress']
            }
        },
        'lodash': {
            'target': {
                'dest': 'build/ng-lodash.js'
            },
            'options': {
                'modifier': 'modern',
                'exports': [
                    'amd',
                    'commonjs',
                    'node'
                ],
                'iife': 'angular.module(\'ngLodash\', [])' +
                    '.constant(\'lodash\', null)' +
                    '.config(function ($provide) { ' +
                        '%output% ' +
                        '$provide.constant(\'lodash\', _);' +
                    '});'
            }
        },
        'ngmin': {
            'dist': {
                'src': 'build/ng-lodash.js',
                'dest': 'build/ng-lodash.js'
            }
        },
        'uglify': {
            'dist': {
                'options': {
                    'compress': {
                        unused: false
                    },
                    'preserveComments': 'some'
                },
                'files': {
                    'build/ng-lodash.min.js': 'build/ng-lodash.js'
                }
            }
        }
    });

    require('load-grunt-tasks')(grunt);

    // Registers a task to run Karma tests and installs any pre-requisites
    // needed.
    grunt.registerTask('test', [
        'jshint',
        'jscs',
        'bower:install',
        'karma:test'
    ]);

    // Registers a task to build the ngLodash module
    grunt.registerTask('build', [
        'lodash',
        'ngmin',
        'uglify'
    ]);

    // Registers a task to build and test ready for distribution
    grunt.registerTask('dist', [
        'build',
        'test'
    ]);

    grunt.registerTask('default', ['build']);
};
