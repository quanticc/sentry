(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('game-server', {
            parent: 'entity',
            url: '/game-server',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'GameServers'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/game-server/game-servers.html',
                    controller: 'GameServerController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
            }
        })
        .state('game-server.refresh', {
            parent: 'game-server',
            url: '/game-server',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'GameServers'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/game-server/game-servers.html',
                    controller: 'GameServerController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['GameServer', function(GameServer) {
                    return GameServer.refresh().$promise;
                }]
            },
            onEnter: function($state) {
                $state.go('^', {}, {reload: true});
            }
        })
        .state('game-server-detail', {
            parent: 'entity',
            url: '/game-server/{id}',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'GameServer'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/game-server/game-server-detail.html',
                    controller: 'GameServerDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'GameServer', function($stateParams, GameServer) {
                    return GameServer.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'game-server',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('game-server-detail.edit', {
            parent: 'game-server-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game-server/game-server-dialog.html',
                    controller: 'GameServerDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['GameServer', function(GameServer) {
                            return GameServer.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('game-server.new', {
            parent: 'game-server',
            url: '/new',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game-server/game-server-dialog.html',
                    controller: 'GameServerDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                address: null,
                                name: null,
                                ping: null,
                                players: null,
                                maxPlayers: null,
                                map: null,
                                version: null,
                                rconPassword: null,
                                svPassword: null,
                                tvPort: null,
                                expires: false,
                                expirationDate: null,
                                expirationCheckDate: null,
                                statusCheckDate: null,
                                lastValidPing: null,
                                lastRconDate: null,
                                lastGameUpdate: null,
                                updating: null,
                                updateAttempts: null,
                                lastUpdateStart: null,
                                lastRconAnnounce: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('game-server', null, { reload: 'game-server' });
                }, function() {
                    $state.go('game-server');
                });
            }]
        })
        .state('game-server.edit', {
            parent: 'game-server',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game-server/game-server-dialog.html',
                    controller: 'GameServerDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['GameServer', function(GameServer) {
                            return GameServer.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('game-server', null, { reload: 'game-server' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('game-server.delete', {
            parent: 'game-server',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game-server/game-server-delete-dialog.html',
                    controller: 'GameServerDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['GameServer', function(GameServer) {
                            return GameServer.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('game-server', null, { reload: 'game-server' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
