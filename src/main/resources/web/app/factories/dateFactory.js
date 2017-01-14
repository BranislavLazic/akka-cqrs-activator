(function() {
  'use strict';
  angular.module('akka-cqrs-activator-ui').factory('dateFactory', [function() {
    var selectedDate;
    return {
      get: function() {
        return selectedDate;
      },
      set: function(date) {
        selectedDate = date;
      }
    }
  }]);
})();