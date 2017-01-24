(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('time-frame', {
            parent: 'entity',
            url: '/time-frame?page&sort&search',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'TimeFrames'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/time-frame/time-frames.html',
                    controller: 'TimeFrameController',
                    controllerAs: 'vm'
                }
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'id,asc',
                    squash: true
                },
                search: null
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtil', function ($stateParams, PaginationUtil) {
                    return {
                        page: PaginationUtil.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtil.parsePredicate($stateParams.sort),
                        ascending: PaginationUtil.parseAscending($stateParams.sort),
                        search: $stateParams.search
                    };
                }],
            }
        })
        .state('time-frame-detail', {
            parent: 'entity',
            url: '/time-frame/{id}',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'TimeFrame'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/time-frame/time-frame-detail.html',
                    controller: 'TimeFrameDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'TimeFrame', function($stateParams, TimeFrame) {
                    return TimeFrame.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'time-frame',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('time-frame-detail.edit', {
            parent: 'time-frame-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/time-frame/time-frame-dialog.html',
                    controller: 'TimeFrameDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['TimeFrame', function(TimeFrame) {
                            return TimeFrame.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('time-frame.new', {
            parent: 'time-frame',
            url: '/new',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/time-frame/time-frame-dialog.html',
                    controller: 'TimeFrameDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                subscriber: null,
                                start: null,
                                end: null,
                                inclusive: false,
                                recurrenceValue: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('time-frame', null, { reload: 'time-frame' });
                }, function() {
                    $state.go('time-frame');
                });
            }]
        })
        .state('time-frame.edit', {
            parent: 'time-frame',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/time-frame/time-frame-dialog.html',
                    controller: 'TimeFrameDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['TimeFrame', function(TimeFrame) {
                            return TimeFrame.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('time-frame', null, { reload: 'time-frame' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('time-frame.delete', {
            parent: 'time-frame',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/time-frame/time-frame-delete-dialog.html',
                    controller: 'TimeFrameDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['TimeFrame', function(TimeFrame) {
                            return TimeFrame.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('time-frame', null, { reload: 'time-frame' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
