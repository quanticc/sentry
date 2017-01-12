(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('subscriber', {
            parent: 'entity',
            url: '/subscriber?page&sort&search',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'Subscribers'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/subscriber/subscribers.html',
                    controller: 'SubscriberController',
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
        .state('subscriber-detail', {
            parent: 'entity',
            url: '/subscriber/{id}',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'Subscriber'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/subscriber/subscriber-detail.html',
                    controller: 'SubscriberDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Subscriber', function($stateParams, Subscriber) {
                    return Subscriber.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'subscriber',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('subscriber-detail.edit', {
            parent: 'subscriber-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/subscriber/subscriber-dialog.html',
                    controller: 'SubscriberDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Subscriber', function(Subscriber) {
                            return Subscriber.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('subscriber.new', {
            parent: 'subscriber',
            url: '/new',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/subscriber/subscriber-dialog.html',
                    controller: 'SubscriberDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                channel: null,
                                type: null,
                                typeParameters: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('subscriber', null, { reload: 'subscriber' });
                }, function() {
                    $state.go('subscriber');
                });
            }]
        })
        .state('subscriber.edit', {
            parent: 'subscriber',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/subscriber/subscriber-dialog.html',
                    controller: 'SubscriberDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Subscriber', function(Subscriber) {
                            return Subscriber.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('subscriber', null, { reload: 'subscriber' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('subscriber.delete', {
            parent: 'subscriber',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/subscriber/subscriber-delete-dialog.html',
                    controller: 'SubscriberDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Subscriber', function(Subscriber) {
                            return Subscriber.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('subscriber', null, { reload: 'subscriber' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
