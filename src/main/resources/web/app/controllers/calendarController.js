(function() {
  'use strict';

  angular.module('akka-cqrs-activator-ui').controller('CalendarController',
          ['$scope', 'moment', 'dateFactory', '$location', function($scope, moment, dateFactory, $location) {
            var date = new Date();
            $scope.calendarTitle = getFormattedDate(date);
            $scope.calendarView = 'month';
            $scope.viewDate = date;

            $scope.events = [];

            $scope.onDayClick = function(calendarDate, calendarCell) {
              dateFactory.set(calendarDate);
              $location.url('/daily-issues')
            };

            $scope.changeDate = function(viewDate) {
              console.log(viewDate);
              $scope.calendarTitle = getFormattedDate(viewDate)
            };

            function getFormattedDate(date) {
              return moment(date).format('MMMM YYYY');
            }

          }]);
})();