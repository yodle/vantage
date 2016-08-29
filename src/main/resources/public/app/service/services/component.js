define([
    'service/module'
], function(service) {
    service.factory('componentService', [
        '$http',
        function($http) {

            var baseUrl = "/api/v1/components";

            var getComponent = function(component) {
                return $http.get(baseUrl + '/' + component);
            };

            var getAllComponents = function() {
                return $http.get(baseUrl);
            };

            var getAllVersions = function(component) {
                return $http.get(baseUrl + '/' + component + '/versions');
            };

            return {
                getComponent: getComponent,
                getAllComponents: getAllComponents,
                getAllVersions: getAllVersions
            }
        }
    ]);
});
