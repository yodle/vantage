define([
    'service/module'
], function(service) {
    service.controller('issuesController', [
        '$scope', '$routeParams', 'issueService',
        function($scope, $routeParams, issueService) {
            $scope.search = [];
            $scope.issues = [];
            
            issueService.getIssues().then(function(response) {
                $scope.issues = response.data;
            });
        }
    ]);
});
