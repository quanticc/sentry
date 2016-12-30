(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('bot', {
            parent: 'entity',
            url: '/bot?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Bots'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/bot/bots.html',
                    controller: 'BotController',
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
        .state('bot-detail', {
            parent: 'entity',
            url: '/bot/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Bot'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/bot/bot-detail.html',
                    controller: 'BotDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Bot', function($stateParams, Bot) {
                    return Bot.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'bot',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('bot-detail.edit', {
            parent: 'bot-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/bot/bot-dialog.html',
                    controller: 'BotDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Bot', function(Bot) {
                            return Bot.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('bot.new', {
            parent: 'bot',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/bot/bot-dialog.html',
                    controller: 'BotDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                token: null,
                                autoLogin: null,
                                daemon: null,
                                maxMissedPings: null,
                                maxReconnectAttempts: null,
                                shardCount: null,
                                primary: null,
                                tags: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('bot', null, { reload: 'bot' });
                }, function() {
                    $state.go('bot');
                });
            }]
        })
        .state('bot.edit', {
            parent: 'bot',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/bot/bot-dialog.html',
                    controller: 'BotDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Bot', function(Bot) {
                            return Bot.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('bot', null, { reload: 'bot' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('bot.delete', {
            parent: 'bot',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/bot/bot-delete-dialog.html',
                    controller: 'BotDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Bot', function(Bot) {
                            return Bot.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('bot', null, { reload: 'bot' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
