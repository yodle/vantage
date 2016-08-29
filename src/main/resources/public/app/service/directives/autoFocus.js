define([
    'service/module'
], function(service) {
  service.directive('autoFocus', function() {
    return {
      link: function(scope, element, attrs) {
        element[0].focus();
      }
    };
  });
});