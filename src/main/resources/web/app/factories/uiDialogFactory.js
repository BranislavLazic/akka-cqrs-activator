(function () {
  'use strict';

  angular.module('akka-cqrs-activator-ui').factory('uiDialog',
    ['$uibModal', '$uibModalStack', 'issueFactory', function ($uibModal, $uibModalStack, issueFactory) {
      return {
        show: function show(action, onSaveCallback) {
          return $uibModal.open({
            templateUrl: 'app/views/modal-dialog.html',
            controller: function ($scope) {
              var vm = this;
              $scope.issue = issueFactory.getSelectedIssuePayload();

              vm.action = action;
              $scope.onSave = function () {
                onSaveCallback($scope.issue);
                $uibModalStack.dismissAll('close');
              },
              $scope.onClose = function() {
                issueFactory.setSelectedIssuePayload({ summary: '', description: '' });
                $uibModalStack.dismissAll('close');
              }
            },
            controllerAs: 'uiDialogCtrl'
          });
        }
      };
    }]);
})();