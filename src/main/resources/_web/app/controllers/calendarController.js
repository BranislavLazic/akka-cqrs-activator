(function () {
  'use strict';

  angular.module('akka-cqrs-activator-ui').controller('CalendarController',
    ['$scope', 'moment', '$location', function ($scope, moment, $location) {
      var date = new Date();
      $scope.calendarTitle = getFormattedDate(date);
      $scope.calendarView = 'month';
      $scope.viewDate = date;

      $scope.events = [];

      $scope.onDayClick = function (calendarDate, calendarCell) {
        var selectedDate = moment(calendarDate).format('YYYY-MM-DD');
        $location.url('/daily-issues?date=' + selectedDate);
      };

      $scope.changeDate = function (viewDate) {
        $scope.calendarTitle = getFormattedDate(viewDate)
      };

      function getFormattedDate(date) {
        return moment(date).format('MMMM YYYY');
      }

    }]);
})();