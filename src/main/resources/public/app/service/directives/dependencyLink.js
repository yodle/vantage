define([
    'service/module'
], function(service) {
   service.directive('dependencyLink', [
      function() {
          return {
              restrict: 'A',
              templateUrl: 'vantage/app/service/templates/partials/dependencyLink.html',
              scope: {
                  dependency: '='
              }
          };
      }

   ]);
});
