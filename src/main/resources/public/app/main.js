requirejs.config({
  baseUrl: '/vantage',

    // The packages that are independent modules in the eyes of requirejs
    packages: [
        {
            name: 'service',
            location: 'app/service'
        }
    ],

    paths: {
        'a8m.unique': 'vendor/angular-filter/dist/angular-filter',
        'angular': 'vendor/angular/angular',
        'angular-busy': 'vendor/angular-busy/angular-busy',
        'angular-moment': 'vendor/angular-moment/angular-moment',
        'angularAnimate': 'vendor/angular-animate/angular-animate',
        'angularRoutes': 'vendor/angular-route/angular-route',
        'angularSanitize': 'vendor/angular-sanitize/angular-sanitize',
        'ui': 'app/ui',
        'jquery': 'vendor/jquery/dist/jquery',
        'loadCss': 'vendor/loadCSS/loadCSS',
        'moment': 'vendor/moment/moment',
        'ng-tags-input': 'vendor/ng-tags-input/ng-tags-input',
        'text': 'vendor/requirejs-text/text',
        'underscore': 'vendor/underscore/underscore',
        'ui-bootstrap': 'vendor/angular-bootstrap/ui-bootstrap-tpls'
    },

    shim: {
            angular: {
                exports: 'angular',
                deps: ['jquery']
            },
            'a8m.unique': {
                deps: ['angular']
            },
            'angular-moment': {
                deps: ['angular', 'moment']
            },
            angularRoutes: {
                deps: ['angular']
            },
            angularSanitize: {
                deps: ['angular']
            },
            'angularTranslate': {
                deps: ['angular']
            },
            'angular-translate-loader-url': {
                deps: ['angularTranslate']
            },
            'ui': {
                deps: ['angular']
            },
            'angularAnimate': {
                exports: 'angularAnimate',
                deps: ['angular']
            },
            'angular-busy': {
                deps: ['angular', 'angularAnimate']
            },
            'loadCss': {
                exports: 'loadCSS'
            },
            'ui-bootstrap': {
                deps: ['angular']
            }
        }
});

require([
    'angular',
    'ui/main',
    'service'
], function(
    angular,
    ui,
    service
    ) {
    angular.bootstrap(document, [
        service.name,
        ui.name
    ]);
});
