(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('player-count', {
            parent: 'entity',
            url: '/player-count?page&sort&search',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'PlayerCounts'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/player-count/player-counts.html',
                    controller: 'PlayerCountController',
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
        .state('player-count-detail', {
            parent: 'entity',
            url: '/player-count/{id}',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'PlayerCount'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/player-count/player-count-detail.html',
                    controller: 'PlayerCountDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'PlayerCount', function($stateParams, PlayerCount) {
                    return PlayerCount.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'player-count',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('player-count-detail.edit', {
            parent: 'player-count-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/player-count/player-count-dialog.html',
                    controller: 'PlayerCountDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['PlayerCount', function(PlayerCount) {
                            return PlayerCount.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('player-count.new', {
            parent: 'player-count',
            url: '/new',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/player-count/player-count-dialog.html',
                    controller: 'PlayerCountDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                region: null,
                                value: null,
                                timestamp: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('player-count', null, { reload: 'player-count' });
                }, function() {
                    $state.go('player-count');
                });
            }]
        })
        .state('player-count.edit', {
            parent: 'player-count',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/player-count/player-count-dialog.html',
                    controller: 'PlayerCountDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['PlayerCount', function(PlayerCount) {
                            return PlayerCount.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('player-count', null, { reload: 'player-count' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('player-count.delete', {
            parent: 'player-count',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/player-count/player-count-delete-dialog.html',
                    controller: 'PlayerCountDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['PlayerCount', function(PlayerCount) {
                            return PlayerCount.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('player-count', null, { reload: 'player-count' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
