define([
    'service/module'
], function(service) {
    service.factory('versionService', [
        '$http',
        function($http) {

            var baseUrl = "/api/v1/components";

            var getVersion = function(component, version) {
                return $http.get(baseUrl + '/' + component + '/versions/' + version);
            };

            return {
                getVersion: getVersion
            }
        }
    ]);
});
