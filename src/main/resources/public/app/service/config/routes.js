define([
    'service/module'
], function(service) {
    service.config([
        '$routeProvider', '$locationProvider',
        function($routeProvider, $locationProvider) {

        $routeProvider
            .when('/vantage/components', {
                controller: 'componentsController',
                templateUrl: '/vantage/app/service/templates/allComponents.html',
                navId: 'nav-components'
            })
            .when('/vantage/components/:component', {
                controller: 'componentController',
                templateUrl: '/vantage/app/service/templates/component.html',
                navId: 'nav-components'
            })
            .when('/vantage/components/:component/versions/:version', {
                controller: 'versionController',
                templateUrl: '/vantage/app/service/templates/version.html',
                navId: 'nav-components'
            })
                .when('/vantage/issues/:issue', {
                    controller: 'issueController',
                    templateUrl: '/vantage/app/service/templates/issue.html',
                    navId: 'nav-components'
                })
                .when('/vantage/createIssue', {
                    controller: 'createIssueController',
                    templateUrl: '/vantage/app/service/templates/issue.html',
                    navId: 'nav-components'
                })
                 .when('/vantage/createIssue/:component/:version', {
                    controller: 'createIssueController',
                    templateUrl: '/vantage/app/service/templates/issue.html',
                    navId: 'nav-components'
                })
                .when('/vantage/createIssue/:component', {
                   controller: 'createIssueController',
                   templateUrl: '/vantage/app/service/templates/issue.html',
                   navId: 'nav-components'
                })
                .when('/vantage/issues', {
                    controller: 'issuesController',
                    templateUrl: '/vantage/app/service/templates/issues.html',
                    navId: 'nav-components'
                })
            .otherwise({
                redirectTo: '/vantage/components'
            });

        $locationProvider.html5Mode(true);
    }]);
});
