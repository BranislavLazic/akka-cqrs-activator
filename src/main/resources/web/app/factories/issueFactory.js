(function () {
    'use strict';
    angular.module('akka-cqrs-activator-ui').factory('issueFactory', ['$http', function ($http) {
        var apiBase = 'http://localhost:8000';
        var issuesEventStream = new EventSource(apiBase + '/issues/event-stream');
        return {
            findByDate: function (date, callback) {
                return $http.get(apiBase + '/issues/' + date).then(callback);
            },
            addNewIssue: function(issue) {
                return $http.post(apiBase + '/issues', { summary: issue.summary, description: issue.description })
            },
            closeIssue: function(date, id) {
                return $http.put(apiBase + '/issues/' + date + '/' + id);
            },
            deleteIssue: function(date, id) {
                return $http.delete(apiBase + '/issues/' + date + '/' + id);
            },
            issuesEventStream: function(eventType, callback) {
                issuesEventStream.addEventListener(eventType, callback, false);
            }
        };
    }]);
})();