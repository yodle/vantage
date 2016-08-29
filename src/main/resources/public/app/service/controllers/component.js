define([
    'service/module'
], function(service) {
    service.controller('componentController', [
        '$scope', '$routeParams', 'componentService',
        function($scope, $routeParams, componentService) {
            $scope.name = $routeParams.component;
            $scope.versionsLoaded = false;

            componentService.getComponent($scope.name).then(function(response) {
                $scope.component = response.data;
            });

            componentService.getAllVersions($scope.name).then(function(response) {
                $scope.versions = response.data;
                 $scope.versionsLoaded = true;
            });
        }
    ]);
});
