#!/usr/bin/env python3
"""
Random Forest Model Training Module
Train and optimize random forest models for underwater acoustic communication
"""

import os
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.model_selection import train_test_split, GridSearchCV, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.metrics import mean_squared_error, r2_score, classification_report, confusion_matrix
import joblib
import logging
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Any
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime

class RandomForestTrainer:
    """Random Forest model trainer for underwater acoustic features"""
    
    def __init__(self, model_dir: str = "models"):
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(exist_ok=True)
        
        # Initialize models
        self.regressor = None
        self.classifier = None
        self.scaler = StandardScaler()
        self.label_encoder = LabelEncoder()
        
        # Model parameters
        self.regressor_params = {
            'n_estimators': 100,
            'max_depth': 10,
            'min_samples_split': 5,
            'min_samples_leaf': 2,
            'random_state': 42,
            'n_jobs': -1
        }
        
        self.classifier_params = {
            'n_estimators': 100,
            'max_depth': 8,
            'min_samples_split': 5,
            'min_samples_leaf': 2,
            'random_state': 42,
            'n_jobs': -1
        }
        
        # Set up logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        
        # Training history
        self.training_history = {
            'regressor': {},
            'classifier': {}
        }
    
    def prepare_data(self, df: pd.DataFrame, target_cols: List[str] = None) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """Prepare data for training"""
        if df.empty:
            raise ValueError("Input dataframe is empty")
        
        # Remove non-numeric columns and handle missing values
        numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()
        
        # Exclude ID columns
        exclude_cols = ['id', 'timestamp']
        feature_cols = [col for col in numeric_cols if col not in exclude_cols]
        
        if 'env_id' in df.columns:
            feature_cols = [col for col in feature_cols if col != 'env_id']
        
        # Prepare features
        X = df[feature_cols].copy()
        
        # Handle missing values
        X = X.fillna(X.mean())
        
        # Remove columns with zero variance
        variance = X.var()
        X = X.loc[:, variance > 1e-10]
        
        self.logger.info(f"Prepared {X.shape[1]} features from {X.shape[0]} samples")
        
        # Prepare targets
        y = pd.DataFrame()
        if target_cols:
            available_targets = [col for col in target_cols if col in df.columns]
            if available_targets:
                y = df[available_targets].copy()
                y = y.fillna(y.mean())
        
        return X, y
    
    def create_synthetic_targets(self, X: pd.DataFrame) -> pd.DataFrame:
        """Create synthetic target variables for demonstration"""
        np.random.seed(42)
        n_samples = len(X)
        
        # Synthetic targets based on feature combinations
        targets = pd.DataFrame()
        
        # Communication quality (0-1, higher is better)
        targets['comm_quality'] = np.clip(
            0.5 + 0.3 * np.random.randn(n_samples) + 
            0.2 * X.get('ml_propagation_efficiency', np.zeros(n_samples)) -
            0.1 * X.get('ml_channel_complexity', np.zeros(n_samples)),
            0, 1
        )
        
        # Transmission loss prediction (dB)
        base_tl = X.get('shd_mean_tl', 60 * np.ones(n_samples))
        targets['transmission_loss'] = np.clip(
            base_tl + 10 * np.random.randn(n_samples),
            30, 120
        )
        
        # Channel stability (categorical: stable, moderate, unstable)
        stability_score = (
            X.get('ml_environment_variability', np.zeros(n_samples)) +
            X.get('ray_path_complexity', np.zeros(n_samples)) / 10
        )
        
        targets['channel_stability'] = pd.cut(
            stability_score,
            bins=3,
            labels=['stable', 'moderate', 'unstable']
        ).astype(str)
        
        # Signal-to-noise ratio (dB)
        targets['snr'] = np.clip(
            20 + 10 * np.random.randn(n_samples) +
            5 * X.get('ml_signal_quality', np.zeros(n_samples)),
            0, 40
        )
        
        # Optimal frequency (Hz)
        targets['optimal_frequency'] = np.clip(
            X.get('env_frequency', 1000 * np.ones(n_samples)) + 
            500 * np.random.randn(n_samples),
            500, 5000
        )
        
        self.logger.info(f"Created {len(targets.columns)} synthetic targets")
        return targets
    
    def train_regressor(self, X: pd.DataFrame, y: pd.Series, 
                       optimize_params: bool = True) -> Dict[str, Any]:
        """Train random forest regressor"""
        self.logger.info(f"Training regressor with {X.shape[1]} features")
        
        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42
        )
        
        # Scale features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Optimize parameters if requested
        if optimize_params:
            self.logger.info("Optimizing regressor parameters...")
            param_grid = {
                'n_estimators': [50, 100, 200],
                'max_depth': [5, 10, 15, None],
                'min_samples_split': [2, 5, 10],
                'min_samples_leaf': [1, 2, 4]
            }
            
            rf = RandomForestRegressor(random_state=42, n_jobs=-1)
            grid_search = GridSearchCV(
                rf, param_grid, cv=5, scoring='r2', n_jobs=1, verbose=1
            )
            grid_search.fit(X_train_scaled, y_train)
            
            self.regressor = grid_search.best_estimator_
            best_params = grid_search.best_params_
            self.logger.info(f"Best parameters: {best_params}")
        else:
            self.regressor = RandomForestRegressor(**self.regressor_params)
            self.regressor.fit(X_train_scaled, y_train)
        
        # Make predictions
        y_pred_train = self.regressor.predict(X_train_scaled)
        y_pred_test = self.regressor.predict(X_test_scaled)
        
        # Calculate metrics
        metrics = {
            'train_r2': r2_score(y_train, y_pred_train),
            'test_r2': r2_score(y_test, y_pred_test),
            'train_rmse': np.sqrt(mean_squared_error(y_train, y_pred_train)),
            'test_rmse': np.sqrt(mean_squared_error(y_test, y_pred_test)),
            'feature_importance': dict(zip(X.columns, self.regressor.feature_importances_))
        }
        
        # Cross-validation
        cv_scores = cross_val_score(self.regressor, X_train_scaled, y_train, cv=5, scoring='r2')
        metrics['cv_r2_mean'] = cv_scores.mean()
        metrics['cv_r2_std'] = cv_scores.std()
        
        # Store training history
        self.training_history['regressor'] = {
            'timestamp': datetime.now(),
            'target': y.name if hasattr(y, 'name') else 'unknown',
            'metrics': metrics,
            'feature_names': X.columns.tolist()
        }
        
        self.logger.info(f"Regressor training completed. Test R²: {metrics['test_r2']:.4f}")
        
        # Return both model and metrics
        return {
            'model': self.regressor,
            'metrics': metrics,
            'best_score': metrics['test_r2']
        }
    
    def train_classifier(self, X: pd.DataFrame, y: pd.Series, 
                        optimize_params: bool = True) -> Dict[str, Any]:
        """Train random forest classifier"""
        self.logger.info(f"Training classifier with {X.shape[1]} features")
        
        # Encode labels
        y_encoded = self.label_encoder.fit_transform(y)
        
        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
        )
        
        # Scale features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Optimize parameters if requested
        if optimize_params:
            self.logger.info("Optimizing classifier parameters...")
            param_grid = {
                'n_estimators': [50, 100, 200],
                'max_depth': [5, 8, 12, None],
                'min_samples_split': [2, 5, 10],
                'min_samples_leaf': [1, 2, 4]
            }
            
            rf = RandomForestClassifier(random_state=42, n_jobs=-1)
            grid_search = GridSearchCV(
                rf, param_grid, cv=5, scoring='accuracy', n_jobs=1, verbose=1
            )
            grid_search.fit(X_train_scaled, y_train)
            
            self.classifier = grid_search.best_estimator_
            best_params = grid_search.best_params_
            self.logger.info(f"Best parameters: {best_params}")
        else:
            self.classifier = RandomForestClassifier(**self.classifier_params)
            self.classifier.fit(X_train_scaled, y_train)
        
        # Make predictions
        y_pred_train = self.classifier.predict(X_train_scaled)
        y_pred_test = self.classifier.predict(X_test_scaled)
        
        # Calculate metrics
        train_accuracy = self.classifier.score(X_train_scaled, y_train)
        test_accuracy = self.classifier.score(X_test_scaled, y_test)
        
        metrics = {
            'train_accuracy': train_accuracy,
            'test_accuracy': test_accuracy,
            'feature_importance': dict(zip(X.columns, self.classifier.feature_importances_)),
            'classification_report': classification_report(
                self.label_encoder.inverse_transform(y_test), 
                self.label_encoder.inverse_transform(y_pred_test),
                output_dict=True
            )
        }
        
        # Cross-validation
        cv_scores = cross_val_score(self.classifier, X_train_scaled, y_train, cv=5)
        metrics['cv_accuracy_mean'] = cv_scores.mean()
        metrics['cv_accuracy_std'] = cv_scores.std()
        
        # Store training history
        self.training_history['classifier'] = {
            'timestamp': datetime.now(),
            'target': y.name if hasattr(y, 'name') else 'unknown',
            'metrics': metrics,
            'feature_names': X.columns.tolist(),
            'classes': self.label_encoder.classes_.tolist()
        }
        
        self.logger.info(f"Classifier training completed. Test accuracy: {metrics['test_accuracy']:.4f}")
        
        # Return both model and metrics
        return {
            'model': self.classifier,
            'metrics': metrics,
            'best_score': metrics['test_accuracy']
        }
    
    def train_multiple_targets(self, X: pd.DataFrame, y: pd.DataFrame) -> Dict[str, Any]:
        """Train models for multiple target variables"""
        results = {}
        
        for target_col in y.columns:
            self.logger.info(f"Training models for target: {target_col}")
            
            target_data = y[target_col].dropna()
            X_target = X.loc[target_data.index]
            
            if target_data.dtype == 'object' or len(target_data.unique()) < 10:
                # Classification task
                try:
                    metrics = self.train_classifier(X_target, target_data, optimize_params=False)
                    results[f"{target_col}_classification"] = metrics
                except Exception as e:
                    self.logger.error(f"Error training classifier for {target_col}: {e}")
            else:
                # Regression task
                try:
                    metrics = self.train_regressor(X_target, target_data, optimize_params=False)
                    results[f"{target_col}_regression"] = metrics
                except Exception as e:
                    self.logger.error(f"Error training regressor for {target_col}: {e}")
        
        return results
    
    def save_models(self, suffix: str = "") -> bool:
        """Save trained models and scalers"""
        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            
            if self.regressor:
                regressor_path = self.model_dir / f"rf_regressor_{timestamp}{suffix}.joblib"
                joblib.dump(self.regressor, regressor_path)
                self.logger.info(f"Regressor saved to {regressor_path}")
            
            if self.classifier:
                classifier_path = self.model_dir / f"rf_classifier_{timestamp}{suffix}.joblib"
                joblib.dump(self.classifier, classifier_path)
                self.logger.info(f"Classifier saved to {classifier_path}")
            
            # Save scaler and label encoder
            scaler_path = self.model_dir / f"scaler_{timestamp}{suffix}.joblib"
            joblib.dump(self.scaler, scaler_path)
            
            if len(self.label_encoder.classes_) > 0:
                encoder_path = self.model_dir / f"label_encoder_{timestamp}{suffix}.joblib"
                joblib.dump(self.label_encoder, encoder_path)
            
            # Save training history
            history_path = self.model_dir / f"training_history_{timestamp}{suffix}.joblib"
            joblib.dump(self.training_history, history_path)
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error saving models: {e}")
            return False
    
    def load_models(self, regressor_path: str = None, classifier_path: str = None, 
                   scaler_path: str = None, encoder_path: str = None) -> bool:
        """Load trained models"""
        try:
            if regressor_path and Path(regressor_path).exists():
                self.regressor = joblib.load(regressor_path)
                self.logger.info(f"Regressor loaded from {regressor_path}")
            
            if classifier_path and Path(classifier_path).exists():
                self.classifier = joblib.load(classifier_path)
                self.logger.info(f"Classifier loaded from {classifier_path}")
            
            if scaler_path and Path(scaler_path).exists():
                self.scaler = joblib.load(scaler_path)
                self.logger.info(f"Scaler loaded from {scaler_path}")
            
            if encoder_path and Path(encoder_path).exists():
                self.label_encoder = joblib.load(encoder_path)
                self.logger.info(f"Label encoder loaded from {encoder_path}")
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error loading models: {e}")
            return False
    
    def predict(self, X: pd.DataFrame, model_type: str = 'regressor') -> np.ndarray:
        """Make predictions with trained models"""
        if model_type == 'regressor' and self.regressor:
            X_scaled = self.scaler.transform(X)
            return self.regressor.predict(X_scaled)
        elif model_type == 'classifier' and self.classifier:
            X_scaled = self.scaler.transform(X)
            predictions = self.classifier.predict(X_scaled)
            return self.label_encoder.inverse_transform(predictions)
        else:
            raise ValueError(f"Model {model_type} not trained or invalid type")
    
    def get_feature_importance(self, model_type: str = 'regressor', top_n: int = 20) -> pd.DataFrame:
        """Get feature importance from trained model"""
        if model_type == 'regressor' and self.regressor:
            model = self.regressor
            feature_names = self.training_history['regressor'].get('feature_names', [])
        elif model_type == 'classifier' and self.classifier:
            model = self.classifier
            feature_names = self.training_history['classifier'].get('feature_names', [])
        else:
            raise ValueError(f"Model {model_type} not trained or invalid type")
        
        importance_df = pd.DataFrame({
            'feature': feature_names,
            'importance': model.feature_importances_
        }).sort_values('importance', ascending=False)
        
        return importance_df.head(top_n)
    
    def plot_feature_importance(self, model_type: str = 'regressor', top_n: int = 15, 
                               save_path: str = None):
        """Plot feature importance"""
        importance_df = self.get_feature_importance(model_type, top_n)
        
        plt.figure(figsize=(10, 8))
        sns.barplot(data=importance_df, x='importance', y='feature')
        plt.title(f'Top {top_n} Feature Importance ({model_type.title()})')
        plt.xlabel('Importance')
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            self.logger.info(f"Feature importance plot saved to {save_path}")
        
        plt.show()
    
    def generate_report(self) -> str:
        """Generate training report"""
        report = []
        report.append("=== Random Forest Training Report ===")
        report.append(f"Generated at: {datetime.now()}")
        report.append("")
        
        # Regressor report
        if 'regressor' in self.training_history and self.training_history['regressor']:
            reg_history = self.training_history['regressor']
            report.append("=== Regressor Results ===")
            report.append(f"Target: {reg_history.get('target', 'Unknown')}")
            
            metrics = reg_history.get('metrics', {})
            report.append(f"Test R²: {metrics.get('test_r2', 0):.4f}")
            report.append(f"Test RMSE: {metrics.get('test_rmse', 0):.4f}")
            report.append(f"CV R² (mean ± std): {metrics.get('cv_r2_mean', 0):.4f} ± {metrics.get('cv_r2_std', 0):.4f}")
            report.append("")
        
        # Classifier report
        if 'classifier' in self.training_history and self.training_history['classifier']:
            cls_history = self.training_history['classifier']
            report.append("=== Classifier Results ===")
            report.append(f"Target: {cls_history.get('target', 'Unknown')}")
            
            metrics = cls_history.get('metrics', {})
            report.append(f"Test Accuracy: {metrics.get('test_accuracy', 0):.4f}")
            report.append(f"CV Accuracy (mean ± std): {metrics.get('cv_accuracy_mean', 0):.4f} ± {metrics.get('cv_accuracy_std', 0):.4f}")
            
            classes = cls_history.get('classes', [])
            if classes:
                report.append(f"Classes: {', '.join(classes)}")
            report.append("")
        
        return "\n".join(report)

if __name__ == "__main__":
    # Example usage with synthetic data
    trainer = RandomForestTrainer()
    
    # Create sample data
    np.random.seed(42)
    n_samples = 100
    n_features = 20
    
    # Sample feature data
    X = pd.DataFrame(
        np.random.randn(n_samples, n_features),
        columns=[f'feature_{i}' for i in range(n_features)]
    )
    
    # Add some meaningful features
    X['ml_propagation_efficiency'] = np.random.uniform(0, 1, n_samples)
    X['ml_channel_complexity'] = np.random.uniform(0, 10, n_samples)
    X['shd_mean_tl'] = np.random.uniform(40, 100, n_samples)
    
    # Create synthetic targets
    y = trainer.create_synthetic_targets(X)
    
    print("Training models with synthetic data...")
    results = trainer.train_multiple_targets(X, y)
    
    # Save models
    trainer.save_models("_synthetic")
    
    # Generate report
    print("\n" + trainer.generate_report())
    
    print(f"\nTraining completed. Results for {len(results)} models:")
    for model_name, metrics in results.items():
        if 'test_r2' in metrics:
            print(f"{model_name}: R² = {metrics['test_r2']:.4f}")
        elif 'test_accuracy' in metrics:
            print(f"{model_name}: Accuracy = {metrics['test_accuracy']:.4f}")