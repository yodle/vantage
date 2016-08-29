define([
    'service/module'
], function(service) {
    service.controller('createIssueController', [
        '$scope', '$routeParams', '$location', 'issueService',
        function($scope, $routeParams, $location, issueService) {
            $scope.levels = ['DEPRECATION', 'MINOR', 'MAJOR', 'CRITICAL'];
            $scope.editMode = true;
            $scope.createMode = true;
            $scope.issue = {
                affectsVersion : {
                    component : $routeParams.component,
                    version : $routeParams.version
                }
            };

            $scope.save = function() {
                if ($scope.issue.fixVersion !== undefined && $scope.issue.fixVersion.component === undefined) {
                    $scope.issue.fixVersion.component = $scope.issue.affectsVersion.component;
                }
                if ($scope.issue.fixVersion !== undefined && (!$scope.issue.fixVersion.version || $scope.issue.fixVersion.version.length === 0)) {
                    $scope.issue.fixVersion = null;
                }
                issueService.saveIssue($scope.issue).then(function(response) {
                    $scope.issue = response.data;
                    $location.path("/vantage/issues/" + $scope.issue.id);
                });
            };
        }
    ]);
});
