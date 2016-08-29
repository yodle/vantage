define([
    'service/module'
], function(service) {
    service.controller('versionController', [
        '$scope', '$routeParams', 'versionService',
        function($scope, $routeParams, versionService) {
            function determinePriority(dependency) {
                if (dependency.version.directIssues && dependency.version.directIssues.length > 0) {
                    return 2;
                }

                if (dependency.version.indirectIssues && dependency.version.indirectIssues.length > 0) {
                    return 1;
                }

                return 0
            }

            function sort(allDependencies) {
                if (!Array.isArray(allDependencies)) {
                    return []; // You lose, game over
                }

                return allDependencies.sort(function(a, b) {
                    var priorityDiff = determinePriority(b) - determinePriority(a);

                    if (priorityDiff != 0) {
                        return priorityDiff;
                    }

                    return a.version.component.localeCompare(b.version.component);
                });
            }

            $scope.name = $routeParams.component;
            $scope.versionName = $routeParams.version;

            $scope.search = [];
            $scope.version = {};
            $scope.versionLoaded = false;

            versionService.getVersion($scope.name, $scope.versionName).then(function(response) {
                $scope.version = response.data;
                $scope.version.resolvedDependencies = sort($scope.version.resolvedDependencies);
                $scope.version.requestedDependencies = sort($scope.version.requestedDependencies);
                $scope.versionLoaded = true;
            });
        }
    ]);
});
