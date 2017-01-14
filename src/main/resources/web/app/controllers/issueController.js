(function () {
  'use strict';
  angular.module('akka-cqrs-activator-ui').controller(
    'IssueController',
    ['$scope', 'dateFactory', 'issueFactory', 'uiDialog', 'lodash',
      function ($scope, dateFactory, issueFactory, uiDialog, lodash) {
        $scope.selectedDate = getFormattedDate();
        $scope.issues = [];
        $scope.onAddNewIssue = function () {
          uiDialog.show('Create a new issue', '', function (issue) {
            issueFactory.addNewIssue(issue);
          });
        };
        var currentDate = moment(dateFactory.get()).format('YYYY-MM-DD');
        issueFactory.findByDate(currentDate, function (response) {
          $scope.issues = response.data;
        });

        $scope.onCloseIssue = function (id) {
          issueFactory.closeIssue(currentDate, id);
        };

        $scope.onDeleteIssue = function (id) {
          issueFactory.deleteIssue(currentDate, id);
        };

        issueFactory.issuesEventStream('issue-created', function (event) {
          $scope.$apply(function () {
            var data = JSON.parse(event.data);
            $scope.issues.push(data);
          });
        });

        issueFactory.issuesEventStream('issue-closed', function (event) {
          $scope.$apply(function () {
            var eventData = JSON.parse(event.data);
            $scope.issues = lodash.map($scope.issues, function (issue) {
              if(issue.id === eventData.id) {
                issue.status = "CLOSED";
              }
              return issue;
            });
          });
        });

        issueFactory.issuesEventStream('issue-deleted', function (event) {
          $scope.$apply(function () {
            var eventData = JSON.parse(event.data);
            $scope.issues = lodash.filter($scope.issues, function (issue) {
              return issue.id != eventData.id;
            });
          });
        });

        function getFormattedDate() {
          return moment(dateFactory.get()).format('DD MMMM YYYY')
        }
      }]);
})();