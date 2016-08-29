define([
    'angular',
    'text!app/ui/components/pagination/templates/paging-ctrl.html',
    'text!app/ui/components/pagination/templates/pager-ctrl.html'
], function(angular, pagingCtrlTmpl, pagerCtrlTmpl) {
    "use strict";

    var module = angular.module('vantageUI');

    module.filter('offset', function() {
        return function(input, start) {
            if (input) {
                start = parseInt(start, 10);
                return input.slice(start);
            }
            else {
                return [];
            }
        };
    });

    module.factory('Pager', [function() {
        return function() {
            this.items = [];
            this.page = 1;
            this.pageSize = 10;

            this.init = function(numPerPage) {
                this.pageSize = parseInt(numPerPage, 10);
            };

            this.pageStart = function() {
                return Math.min(this.pageOffset() + 1, this.items.length);
            };

            this.pageEnd = function() {
                return Math.min(this.pageOffset() + this.pageSize, this.items.length);
            };

            this.pageOffset = function() {
                return (this.page - 1) * this.pageSize;
            };

            this.setPage = function(p) {
                this.page = Math.max(Math.min(this.numPages(), p), 1);
            };

            this.nextPage = function(e) {
                e.preventDefault();
                this.setPage(this.page + 1);
            };

            this.prevPage = function(e) {
                e.preventDefault();
                this.setPage(this.page - 1);
            };

            this.isFirstPage = function() {
                return this.page <= 1;
            };

            this.isLastPage = function() {
                return this.page >= this.numPages();
            };

            this.numPages = function() {
                return Math.ceil(this.items.length / this.pageSize);
            };
        };
    }]);

    module.controller('paginate', ['$scope', 'Pager', '$attrs', function($scope, Pager, $attrs) {
        $scope.pager = new Pager();

        $scope.$watch('pager.items', function(newVal, oldVal) {
            var key = $attrs.paginationKey

            if (key) {
                var newKeys = _.pluck(newVal, key);
                var oldKeys = _.pluck(oldVal, key);
                if (!_.isEqual(newKeys, oldKeys)) {
                    $scope.pager.setPage(1);
                }
            }
            else {
                $scope.pager.setPage(1);
            }
        }, true);

    }]);

    module.directive('prevPage', [function() {
        return {
            restrict: 'AE',
            replace: true,
            require: '^paginate',
            template: '<a href="#" class="pull-right" ng-click="pager.prevPage($event)" ng-class="{disabled: pager.isFirstPage()}"><span class="glyphicon glyphicon-chevron-left"></span></a>',
        };
    }]);

    module.directive('nextPage', [function() {
        return {
            restrict: 'AE',
            replace: true,
            require: '^paginate',
            template: '<a href="#" class="pull-right"  ng-click="pager.nextPage($event)" ng-class="{disabled: pager.isLastPage()}"><span class="glyphicon glyphicon-chevron-right"></span></a>',
        };
    }]);

    module.directive('pagingInfo', [function() {
        return {
            restrict: 'AE',
            replace: true,
            require: '^paginate',
            template: ' <div class="text pull-left">Displaying {{pager.pageStart()}} to {{pager.pageEnd()}} of {{pager.items.length}}</div>',
        };
    }]);

    module.directive('pagerInfo', [function() {
        return {
            restrict: 'AE',
            replace: true,
            require: '^paginate',
            template: ' <div class="text pull-left">Page {{pager.page}} of {{pager.numPages()}}</div>',
        };
    }]);

    module.directive('pagingCtrl', [function() {
        return {
            restrict: 'AE',
            replace: true,
            require: '^paginate',
            template: pagingCtrlTmpl,
        };
    }]);

    module.directive('pagerCtrl', [function(){
        return {
            restrict: 'AE',
            replace: true,
            required: '^paginate',
            template: pagerCtrlTmpl,
        };
    }]);
});
