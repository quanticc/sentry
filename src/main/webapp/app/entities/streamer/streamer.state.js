(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('streamer', {
            parent: 'entity',
            url: '/streamer?page&sort&search',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'Streamers'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/streamer/streamers.html',
                    controller: 'StreamerController',
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
        .state('streamer-detail', {
            parent: 'entity',
            url: '/streamer/{id}',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'Streamer'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/streamer/streamer-detail.html',
                    controller: 'StreamerDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Streamer', function($stateParams, Streamer) {
                    return Streamer.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'streamer',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('streamer-detail.edit', {
            parent: 'streamer-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/streamer/streamer-dialog.html',
                    controller: 'StreamerDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Streamer', function(Streamer) {
                            return Streamer.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('streamer.new', {
            parent: 'streamer',
            url: '/new',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/streamer/streamer-dialog.html',
                    controller: 'StreamerDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                provider: 'Twitch',
                                name: null,
                                league: null,
                                division: null,
                                titleFilter: null,
                                announcement: null,
                                lastAnnouncement: null,
                                lastStreamId: null,
                                enabled: true,
                                embedFields: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('streamer', null, { reload: 'streamer' });
                }, function() {
                    $state.go('streamer');
                });
            }]
        })
        .state('streamer.edit', {
            parent: 'streamer',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/streamer/streamer-dialog.html',
                    controller: 'StreamerDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Streamer', function(Streamer) {
                            return Streamer.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('streamer', null, { reload: 'streamer' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('streamer.delete', {
            parent: 'streamer',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_SUPPORT']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/streamer/streamer-delete-dialog.html',
                    controller: 'StreamerDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Streamer', function(Streamer) {
                            return Streamer.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('streamer', null, { reload: 'streamer' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
