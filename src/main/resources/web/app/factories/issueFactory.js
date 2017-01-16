(function () {
    'use strict';
    angular.module('akka-cqrs-activator-ui').factory('issueFactory', ['$http', function ($http) {
        var apiBase = 'http://localhost:8000';
        var selectedIssuePayload = {
            summary: '',
            description: ''
        };
        var issuesEventStream = new EventSource(apiBase + '/issues/event-stream');
        return {
            findByDate: function (date, callback, errorCallback) {
                return $http.get(apiBase + '/issues/' + date).then(callback, errorCallback);
            },
            addNewIssue: function (dateOfCreation, issue) {
                return $http.post(apiBase + '/issues', { date: dateOfCreation, summary: issue.summary, description: issue.description })
            },
            updateIssue: function (date, id, summary, description) {
                return $http.put(apiBase + '/issues/' + date + '/' + id, { summary: summary, description: description });
            },
            closeIssue: function (date, id) {
                return $http.put(apiBase + '/issues/' + date + '/' + id);
            },
            deleteIssue: function (date, id) {
                return $http.delete(apiBase + '/issues/' + date + '/' + id);
            },
            issuesEventStream: function (eventType, callback) {
                issuesEventStream.addEventListener(eventType, callback, false);
            },
            setSelectedIssuePayload: function (issue) {
                selectedIssuePayload = issue;
            },
            getSelectedIssuePayload: function () {
                return selectedIssuePayload;
            }
        };
    }]);
})();