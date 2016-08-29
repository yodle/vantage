define([
    'service/module'
], function(service) {
    service.controller('issueController', [
        '$scope', '$routeParams', 'issueService',
        function($scope, $routeParams, issueService) {
            $scope.issueId = $routeParams.issue;

            function refreshIssue() {
                issueService.getIssue($scope.issueId).then(function(response) {
                    $scope.issue = response.data;
                });
            }

            refreshIssue();

            $scope.levels = ['DEPRECATION', 'MINOR', 'MAJOR', 'CRITICAL'];
            
            $scope.editMode = false;
            $scope.createMode = false;
            $scope.switchToEdit = function() {
                $scope.editMode = true
            };

            $scope.save = function() {
                if ($scope.issue.fixVersion !== undefined && $scope.issue.fixVersion.component === undefined) {
                    $scope.issue.fixVersion.component = $scope.issue.affectsVersion.component;
                }

                if ($scope.issue.fixVersion !== undefined && (!$scope.issue.fixVersion.version || $scope.issue.fixVersion.version.length === 0)) {
                    $scope.issue.fixVersion = null;
                }

                issueService.saveIssue($scope.issue).then(function(response) {
                    $scope.issue = response.data
                });

                $scope.editMode = false
            };

            $scope.cancel = function() {
                refreshIssue();
                $scope.editMode = false
            }
        }
    ]);
});
