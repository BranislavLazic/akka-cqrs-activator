(function() {
  'use strict';
  angular.module('akka-cqrs-activator-ui',
          ['ngRoute', 'ngAnimate', 'angularMoment', 'mwl.calendar', 'ui.bootstrap', 'ui.bootstrap.modal', 'ngLodash']).config(
          function($routeProvider) {
            $routeProvider.when('/', {
              templateUrl: 'app/views/calendar.html',
              controller: 'CalendarController'
            }).when('/daily-issues', {
                templateUrl: 'app/views/issues.html',
                controller: 'IssueController'
            });
          });
})();