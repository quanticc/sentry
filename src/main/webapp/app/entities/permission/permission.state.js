(function() {
    'use strict';

    angular
        .module('sentryApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('permission', {
            parent: 'entity',
            url: '/permission?page&sort&search',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'Permissions'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/permission/permissions.html',
                    controller: 'PermissionController',
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
        .state('permission-detail', {
            parent: 'entity',
            url: '/permission/{id}',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'Permission'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/permission/permission-detail.html',
                    controller: 'PermissionDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Permission', function($stateParams, Permission) {
                    return Permission.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'permission',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('permission-detail.edit', {
            parent: 'permission-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/permission/permission-dialog.html',
                    controller: 'PermissionDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Permission', function(Permission) {
                            return Permission.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('permission.new', {
            parent: 'permission',
            url: '/new',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/permission/permission-dialog.html',
                    controller: 'PermissionDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                type: null,
                                role: null,
                                operation: null,
                                resource: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('permission', null, { reload: 'permission' });
                }, function() {
                    $state.go('permission');
                });
            }]
        })
        .state('permission.edit', {
            parent: 'permission',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/permission/permission-dialog.html',
                    controller: 'PermissionDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Permission', function(Permission) {
                            return Permission.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('permission', null, { reload: 'permission' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('permission.delete', {
            parent: 'permission',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/permission/permission-delete-dialog.html',
                    controller: 'PermissionDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Permission', function(Permission) {
                            return Permission.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('permission', null, { reload: 'permission' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
