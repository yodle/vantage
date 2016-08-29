define([
    'service/module'
], function(service) {
    service.controller('componentsController', [
        '$scope', '$routeParams', 'componentService',
        function($scope, $routeParams, componentService) {
            $scope.name = $routeParams.component;

            $scope.search = [];
            $scope.components = [];
            $scope.componentsLoaded = false;
            componentService.getAllComponents().then(function(response) {
                $scope.componentsLoaded = true;
                $scope.components = response.data;
            });
        }
    ]);
});
