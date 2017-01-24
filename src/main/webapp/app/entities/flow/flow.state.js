(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('flow', {
            parent: 'entity',
            url: '/flow?page&sort&search',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'Flows'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/flow/flows.html',
                    controller: 'FlowController',
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
        .state('flow-detail', {
            parent: 'entity',
            url: '/flow/{id}',
            data: {
                authorities: ['ROLE_SUPPORT'],
                pageTitle: 'Flow'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/flow/flow-detail.html',
                    controller: 'FlowDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Flow', function($stateParams, Flow) {
                    return Flow.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'flow',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('flow-detail.edit', {
            parent: 'flow-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_MANAGER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/flow/flow-dialog.html',
                    controller: 'FlowDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Flow', function(Flow) {
                            return Flow.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('flow.new', {
            parent: 'flow',
            url: '/new',
            data: {
                authorities: ['ROLE_MANAGER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/flow/flow-dialog.html',
                    controller: 'FlowDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                input: null,
                                inputParameters: null,
                                message: null,
                                translator: null,
                                output: null,
                                enabled: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('flow', null, { reload: 'flow' });
                }, function() {
                    $state.go('flow');
                });
            }]
        })
        .state('flow.edit', {
            parent: 'flow',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_MANAGER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/flow/flow-dialog.html',
                    controller: 'FlowDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Flow', function(Flow) {
                            return Flow.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('flow', null, { reload: 'flow' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('flow.delete', {
            parent: 'flow',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/flow/flow-delete-dialog.html',
                    controller: 'FlowDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Flow', function(Flow) {
                            return Flow.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('flow', null, { reload: 'flow' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
