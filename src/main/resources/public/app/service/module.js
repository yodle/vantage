define([
    'angular',
    'angular-busy',
    'angular-moment',
    'angularRoutes',
    'angularSanitize',
    'moment',
    'a8m.unique'
], function(angular) {
    return angular.module('service', [
        'cgBusy',
        'ngRoute',
        'ngSanitize',
        'angularMoment',
        'a8m.unique'
    ]);
});
