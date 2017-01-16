(function () {
  'use strict';
  angular.module('akka-cqrs-activator-ui').controller(
    'IssueController',
    ['$scope', 'issueFactory', 'uiDialog', 'lodash', '$routeParams', '$location',
      function ($scope, issueFactory, uiDialog, lodash, $routeParams, $location) {
        var currentDate = $routeParams.date;
        $scope.selectedDate = getFormattedDate();
        $scope.issues = [];

        $scope.onAddNewIssue = function () {
          uiDialog.show('Create a new issue', function (issue) {
            issueFactory.addNewIssue(currentDate, issue);
            issueFactory.setSelectedIssuePayload({ summary: '', description: '' });
          });
        };

        $scope.onUpdateIssue = function (issue) {
          issueFactory.setSelectedIssuePayload({ summary: issue.summary, description: issue.description });

          uiDialog.show('Update an issue', function (updateIssue) {
            issueFactory.updateIssue(issue.date, issue.id, updateIssue.summary, updateIssue.description);
            issueFactory.setSelectedIssuePayload({ summary: '', description: '' });
          });
        };

        issueFactory.findByDate(currentDate, function (response) {
          $scope.issues = response.data;
        }, function (response) { });

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
              if (issue.id === eventData.id) {
                issue.status = "CLOSED";
              }
              return issue;
            });
          });
        });

        issueFactory.issuesEventStream('issue-updated', function (event) {
          $scope.$apply(function () {
            var eventData = JSON.parse(event.data);
            $scope.issues = lodash.map($scope.issues, function (issue) {
              if (issue.id === eventData.id) {
                issue.summary = eventData.summary;
                issue.description = eventData.description;
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

        $scope.onBackToCalendar = function () {
          $location.url('/');
        };

        function getFormattedDate() {
          return moment(currentDate).format('DD MMMM YYYY')
        }
      }]);
})();