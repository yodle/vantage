define([
    'service/module'
], function(service) {
    service.factory('issueService', [
        '$http',
        function($http) {

            var baseUrl = "/api/v1/issues";

            var getIssue = function(issueId) {
                return $http.get(baseUrl + '/' + issueId);
            };

            var getIssues = function() {
                return $http.get(baseUrl);
            };

            var saveIssue = function(issue) {
                return $http.put(baseUrl + '/' + issue.id, issue);
            };

            return {
                getIssue: getIssue,
                getIssues: getIssues,
                saveIssue: saveIssue
            }
        }
    ]);
});
