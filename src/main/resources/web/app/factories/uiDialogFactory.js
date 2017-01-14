(function () {
  'use strict';

  angular.module('akka-cqrs-activator-ui').factory('uiDialog',
    ['$uibModal', '$uibModalStack', function ($uibModal, $uibModalStack) {
      return {
        show: function show(action, event, onSaveCallback) {
          return $uibModal.open({
            templateUrl: 'app/views/modal-dialog.html',
            controller: function ($scope) {
              var vm = this;
              $scope.issue = {
                summary: '',
                description: ''
              };
              vm.action = action;
              vm.event = event;
              $scope.onSave = function () {
                onSaveCallback($scope.issue);
                $uibModalStack.dismissAll('close');
              }
            },
            controllerAs: 'uiDialogCtrl'
          });
        }
      };
    }]);
})();