define([
    'service/module'
], function(service) {
   service.directive('dependencyList', [
      function() {
          return {
              restrict: 'A',
              templateUrl: 'vantage/app/service/templates/partials/dependencyList.html',
              scope: {
                  dependencies: '='
              }
          };
      }

   ]);
});
